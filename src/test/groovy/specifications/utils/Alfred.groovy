package specifications.utils


import io.restassured.authentication.PreemptiveBasicAuthScheme
import io.restassured.builder.RequestSpecBuilder
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import org.awaitility.core.ConditionEvaluationLogger

import static specifications.utils.Properties.account_endpoint
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath

import static org.hamcrest.Matchers.anyOf
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

import static io.restassured.RestAssured.*
import io.restassured.response.Response

import static specifications.utils.Properties.user_name
import static specifications.utils.Properties.user_pswd
import static specifications.utils.Properties.client_id
import static specifications.utils.Properties.client_secret
import static specifications.utils.Properties.mano_url
import static specifications.utils.Properties.vnf_record_endpoint
import static specifications.utils.Properties.order_endpoint
import static specifications.utils.Properties.metric_list_endpoint

class Alfred {

    //CONFIGURATION
    static setPollingDefaults(){
        //Service Order Completion Waiting Setting
        Awaitility.setDefaultTimeout(600, TimeUnit.SECONDS )
        Awaitility.setDefaultPollInterval(15,TimeUnit.SECONDS )
        Awaitility.setDefaultPollDelay(15, TimeUnit.SECONDS)
    }
    static PreemptiveBasicAuthScheme setBasicAuthScheme(){
        //Authentication scheme
        PreemptiveBasicAuthScheme basicAuthScheme = new PreemptiveBasicAuthScheme()
        basicAuthScheme.setUserName(user_name)
        basicAuthScheme.setPassword(user_pswd)
        return basicAuthScheme
    }
    static RequestSpecification setupRequestSpec(PreemptiveBasicAuthScheme basicAuthScheme){

        RequestSpecBuilder builderReq = new RequestSpecBuilder()
        builderReq.setContentType('application/json')
        builderReq.setAuth(basicAuthScheme)
        builderReq.setBaseUri(mano_url)
        builderReq.setBasePath(order_endpoint)
        builderReq.build()
    }
    static ResponseSpecification setupOrderResponseSpec() {
        ResponseSpecBuilder builderRESP = new ResponseSpecBuilder()
        builderRESP.expectBody(matchesJsonSchemaInClasspath("scheme/branch-so-schema.json"))
        builderRESP.expectStatusCode(anyOf(is(200), is(201)))
        builderRESP.build()
    }
    static ResponseSpecification setupAccountResponseSpec() {
        ResponseSpecBuilder builderRESP = new ResponseSpecBuilder()
        builderRESP.expectBody(matchesJsonSchemaInClasspath("scheme/account-schema.json"))
        builderRESP.expectStatusCode(anyOf(is(200), is(201)))
        builderRESP.build()
    }


    //SECTION OF GETs
    static String getToken(){
                given().accept('application/json')
                .contentType('application/x-www-form-urlencoded')
                .auth().preemptive().basic(client_id,client_secret)
                .baseUri(mano_url)
                .basePath('/token')
                .queryParam('grant_type','client_credentials') .log().all()
                .post().then().log().all()
                .assertThat().statusCode(200)
                .extract().path('access_token')
    }
    static Response getNSlist(String access_token,String network_service_endpoint){
                given().contentType('application/json')
                .auth().oauth2(access_token)
                .baseUri(mano_url)
                .basePath(network_service_endpoint)//.log().all()
                .get().then()//.log().all()
                .assertThat().statusCode(200)
                .extract().response()
    }
    static Response getNetworkService(String access_token,String network_service_endpoint, String externalKey){
                given().contentType('application/json')
                .auth().oauth2(access_token)
                .baseUri(mano_url)
                .basePath(network_service_endpoint).log().all()
                .get("$externalKey").then().log().all()
                .assertThat().statusCode(200)
                .extract().response()
    }
    static Response getVNFrecord(String access_token,String nc_object_id){
                given().contentType('application/json')
                .auth().oauth2(access_token)
                .baseUri(mano_url)
                .basePath(vnf_record_endpoint).log().all()
                .get("$nc_object_id/details").then().log().all()
                .assertThat().statusCode(200)
                .extract().response()
    }
    static Response getServiceInventory(RequestSpecification requestSpec, String account_id){
                given().spec(requestSpec)
                .basePath(account_endpoint).log().all()
                .get("$account_id/serviceInstances").then().log().all()
                .assertThat().statusCode(200)
                .extract().response()

    }



    //ORDER STATUS CONTROL
    static getOrderStatus(RequestSpecification requestSpec, String ext_id){
        def response =  given().spec(requestSpec)
                .basePath(order_endpoint)
                .get(ext_id)
                .then()//.log().all()
                .extract().response()
        return [response.path("state"),response.path("orderItem[0].state")].toString()
    }
    static pollForOrderCompletion(RequestSpecification requestSpec, String ext_id){

        Awaitility.with().
                conditionEvaluationListener(new ConditionEvaluationLogger()).
                await().until( { getOrderStatus(requestSpec,ext_id) }, equalTo("[Completed, Completed]"))
    }



    //METRICS
    static getMetricList(RequestSpecification requestSpec){
        given().spec(requestSpec)
                .basePath(metric_list_endpoint).log().all()
                .get().then().log().all()
                .assertThat().statusCode(200)
                .extract().response()
    }
    static getMetricFile(RequestSpecification requestSpec, String file_name){
            given().spec(requestSpec)
                   .basePath(metric_list_endpoint).log().all()
                   .get("$file_name/export").then().log().all()
                   .assertThat().statusCode(200)
                   .extract().response()
    }

    //MISC
    static  printClassPath(classLoader) {

        classLoader.getURLs().each {url-> println "- ${url.toString()}" }

        if (classLoader.parent) { printClassPath(classLoader.parent)}
    }

    static String difference(String str1, String str2) {
        if (str1 == null) { return str2 }
        if (str2 == null) { return str1 }
        int at = indexOfDifference(str1, str2)
        if (at == -1) { return "" }
        return str2.substring(at)
    }

    static int indexOfDifference(String str1, String str2) {
        if (str1 == str2) { return -1 }
        if (str1 == null || str2 == null) { return 0 }
        int i
        for (i = 0; i < str1.length() && i < str2.length(); ++i) { if (str1.charAt(i) != str2.charAt(i)) { break } }
        if (i < str2.length() || i < str1.length()) { return i }
        return -1
    }

}


