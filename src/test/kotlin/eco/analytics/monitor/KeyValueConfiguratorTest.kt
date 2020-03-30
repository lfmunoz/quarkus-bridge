package eco.analytics.monitor

import eco.analytics.kafka.KafkaPublisherBare
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test

import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import io.restassured.RestAssured.`when`
import io.restassured.path.json.JsonPath
import org.hamcrest.CoreMatchers.equalTo
import javax.enterprise.context.ApplicationScoped
import io.quarkus.test.Mock



// https://restfulapi.net/json-jsonpath/
// http://rest-assured.io/





@QuarkusTest
class KeyValueConfiguratorTest {



    @Test
    fun `endpoint set`() {
        val postBody = KeyValueDto(
                key = "keyTest",
                value = "valueTest",
                type = ValueType.STRING
        )
      given().contentType("application/json")
                .body(postBody).`when`()
                .post("/api/set")
              .then()
                .statusCode(200)
                .body("return", equalTo(OK))
    }

    @Test
    fun `endpoint view`() {
        given().contentType("application/json")
                .get("/api/view")
                .then()
                .statusCode(200)
//                .body("return", equalTo(OK))
    }

}