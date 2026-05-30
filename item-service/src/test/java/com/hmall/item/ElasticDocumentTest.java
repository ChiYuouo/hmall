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
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=dev")
public class ElasticDocumentTest {
    private RestHighLevelClient client;
    @Autowired
    private IItemService itemService;

    @Test
    void testConnect(){
        System.out.println("client="+client);
    }

    @Test
    void testIndexDoc() throws IOException {
        //0、准备文档数据
        //0.1根据id查询数据库数据
        Item item = itemService.getById(100000011127L);
        //0.2把数据库数据转成文档数据
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //1.准备Request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2、准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc),XContentType.JSON);
        //3、发送请求
        client.index(request,RequestOptions.DEFAULT);
    }

    @Test
    void testGetDoc() throws IOException {
        // 1.准备Request对象
        GetRequest request = new GetRequest("items").id("100000011127");
        // 2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 3.获取响应结果中的source
        String json = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
        System.out.println("itemDoc= " + itemDoc);
    }

    @Test
    void testDeleteDoc() throws IOException {
        // 1.准备Request对象
        DeleteRequest request = new DeleteRequest("items").id("100000011127");
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }

    @Test
    void testUpadateDoc() throws IOException {
        // 1.准备Request对象
        UpdateRequest request = new UpdateRequest("items","100000011127");
        request.doc(
                "price",25700
        );
        // 2.发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulk() throws IOException {
        int pageNo=1,pageSize=500;
        while (true){
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo,pageSize));
            List<Item> records = page.getRecords();
            if(records==null||records.isEmpty()){
                return;
            }
            // 1.创建Request
            BulkRequest request = new BulkRequest();
            // 2.准备请求参数
            for (Item record : records) {
                request.add(new IndexRequest("items").id(record.getId().toString()).source(JSONUtil.toJsonStr(BeanUtil.copyProperties(record, ItemDoc.class)),XContentType.JSON));
            }
            // 3.发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
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

    private static final String MAPPING_TEMPLATE="{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
