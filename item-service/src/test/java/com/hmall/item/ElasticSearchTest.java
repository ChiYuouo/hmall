package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;

//@SpringBootTest(properties = "spring.profiles.active=dev")
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMathAll() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source()
                .query(QueryBuilders.matchAllQuery());
        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }
    @Test
    void testBool() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
                );
        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        int pageNo=2,pageSize=5;
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source().query(QueryBuilders.matchAllQuery());
        //分页
        request.source().from((pageNo-1)*pageSize).size(pageSize);
        //排序
        request.source().sort("sold", SortOrder.DESC).sort("price",SortOrder.ASC);
        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testHighlight() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));
        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testAgg() throws IOException {
        //1、创建request对象
        SearchRequest request = new SearchRequest("items");
        //2、配置request参数
       request.source().size(0);
       String brandAggName="brandAgg";
       request.source().aggregation(
               AggregationBuilders.terms(brandAggName).field("brand.keyword").size(10)
       );
        //3、发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get(brandAggName);
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println("brand="+bucket.getKeyAsString());
            System.out.println("count="+bucket.getDocCount());
        }
        System.out.println("response = " + response);
    }



    private static void parseResponseResult(SearchResponse response) {
        //4、解析结果
        SearchHits searchHits = response.getHits();
        //4.1总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total="+total);
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if(hfs!=null&&!hfs.isEmpty()){
                HighlightField hf = hfs.get("name");
                String hfName = hf.getFragments()[0].string();
                doc.setName(hfName);
            }
            System.out.println("doc="+doc);
        }
    }

    @BeforeEach
    void setUp() {
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://157.245.206.115:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client!=null){
            client.close();
        }
    }
}
