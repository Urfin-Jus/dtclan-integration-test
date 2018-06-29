package specifications

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import spock.lang.*

import java.util.concurrent.atomic.AtomicInteger

import static io.restassured.RestAssured.*

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath
import static org.hamcrest.Matchers.equalTo
import static specifications.utils.Alfred.*
import static specifications.utils.Properties.account_endpoint
import static specifications.utils.Properties.natCo


class API_Customer_Account_Spec extends Specification {

    @Shared def testRunID;     @Shared def prefix ;  @Shared def counter

    @Shared def customer_A;    @Shared def admin_A
    @Shared def customer_B;    @Shared def admin_B
    @Shared def customer_C;    @Shared def admin_C

    @Shared RequestSpecification requestSpec
    @Shared ResponseSpecification responseSpec_order
    @Shared ResponseSpecification responseSpec_account


    def setupSpec(){
        setPollingDefaults()
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        responseSpec_account = setupAccountResponseSpec()
        responseSpec_order = setupOrderResponseSpec()

        testRunID = new Date().format('MMddhhmm')
        prefix = "API-ACCOUNT-$testRunID"
        counter = new AtomicInteger()

        customer_A = ("$prefix-$testRunID-ACCOUNT_A").toString()
        customer_B = ("$prefix-$testRunID-ACCOUNT_B").toString()
        //customer_A = 'API-ACCOUNT-06220408-06220408-ACCOUNT_A'
        //customer_B = 'API-ACCOUNT-06220408-06220408-ACCOUNT_B'
        customer_C = ("$prefix-$testRunID-ACCOUNT_C").toString()
        admin_A = ("$prefix-$testRunID-ADMIN_A").toString()
        admin_B = ("$prefix-$testRunID-ADMIN_B").toString()
        admin_C = ("$prefix-$testRunID-ADMIN_C").toString()
    }

    @Unroll("Create Account as Test Data: #ca_id , #admin_id")
    def "Create Account. Positive. As Test Data"(){
        setup:

            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredAccount.json').toURI() )
            def json_body = new JsonSlurper().parse(template_file)

            json_body.'customer-id' = ca_id
            json_body.'admin-id' = admin_id
            json_body.status = "active"
            json_body.'tenant-id' = tenant_id

        expect:
            given().spec(requestSpec)
                    .basePath(account_endpoint)
                    .body(json_body).log().all()
                    .post().then().log().all()
                    .assertThat().statusCode(201)
        and:
            given().spec(requestSpec)
                    .basePath(account_endpoint).log().headers()
                    .get("$ca_id").then().log().all()
                    .assertThat()
                    .statusCode(200)
                    .body(matchesJsonSchemaInClasspath("scheme/account-schema.json"))

        where:
            tenant_id = natCo

            ca_id 	  	| admin_id
            customer_A	| admin_A
            customer_B	| admin_B
    }

    def "Terminate Account B. Positive. As Test Data"(){
        expect:
            given().spec(requestSpec)
                .basePath(account_endpoint).log().all()
                .delete(customer_B.toString()).then().log().all()
                .assertThat()
                .statusCode(204)

        and:
        given().spec(requestSpec)
                .basePath(account_endpoint).log().headers()
                .get(customer_B.toString()).then().log().all()
                .assertThat()
                .statusCode(200)
                .body('status',equalTo('defunct'))


    }

    @Issue('Customer Admin ID is mandatory in Interface Agreement, but actually not')
    @Unroll("Create Account. Check if #focus_parameter is mandatory")
    def "Create Account. Mandatory parameters check"(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredAccount.json').toURI() )
            def json_body = new JsonSlurper().parse(template_file)

            json_body.'customer-id' = ca_id
            json_body.'admin-id' = admin_id
            json_body.status = status
            json_body.'tenant-id' = natCo_id

        expect:
            given().spec(requestSpec)
                    .basePath(account_endpoint)
                    .body(json_body).log().all()
                    .post().then().log().all()
                    .assertThat()
                    .statusCode(http_code)
                    .body('error', equalTo(error_message))


        where:
        focus_parameter 	| ca_id 		| natCo_id | status   	| admin_id		|| error_message									|| http_code
        "customer-id"  		| ""			| natCo	   | "active"	| admin_C	    || "Customer ID shouldn't be null or empty"			|| 422
        "tenant-id"			| customer_C	| ""	   | "active" 	| admin_C	    || "Tenant id shouldn't be null or empty"			|| 422
        "status"			| customer_C	| natCo	   | ""			| admin_C	    || "Customer Account status should be active"		|| 400
        //"admin-id"          | customer_C    | natCo    | "active"   | ''            || "Admin User shouldn't be null or empty"          || 400


    }

    @Unroll("Create Account. Check #focus_parameter is unique")
    def "Create. Unique parameters check"(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredAccount.json').toURI() )
            def json_body = new JsonSlurper().parse(template_file)

            json_body.'customer-id' = ca_id
            json_body.'admin-id' = admin_id
            json_body.status = "active"
            json_body.'tenant-id' = natCo

        expect:
            given().spec(requestSpec)
                .basePath(account_endpoint)
                .body(json_body).log().all()
                .post().then().log().all()
                .assertThat()
                .statusCode(400)
                .body('error', equalTo(error_message.toString()))

        and:


        where:
        focus_parameter | ca_id 		| admin_id	|| error_message
        "customer-id"   | customer_A	| admin_C	|| "Customer with External Customer ID = $customer_A already exists"
        "admin-id"      | customer_C	| admin_A	|| "Customer with Admin Login = $admin_A already exists"

    }

    @Unroll("Modify Account. Not Allowed Operation: #title ")
    def "Modify. Not Allowed Operations "(){
        setup:
            def json_body = JsonOutput.toJson(map_param)
            //ValidatableResponse response
            Response    response

        expect:
            switch(title){
                case 'Terminate already defunct Customer Account':
                        given().spec(requestSpec)
                                    .basePath(account_endpoint).log().all()
                                    .delete(ca_id.toString()).then().log().all()
                                    .assertThat()
                                    .statusCode(http_code)
                                    .body('error',equalTo(error_message.toString()))
                        break


                case ['Change tenant-id parameter','Change customer-id parameter']:
                        given().spec(requestSpec)
                            .basePath(account_endpoint)
                            //.accept('text/plain')
                            .body(json_body).log().all()
                            .patch(ca_id.toString()).then().log().all()
                            .assertThat()
                            .statusCode(http_code)
                            .body(equalTo(error_message.toString()))
                        break

                default:
                        given().spec(requestSpec)
                                    .basePath(account_endpoint)
                                    .body(json_body).log().all()
                                    .patch(ca_id.toString()).then().log().all()
                                    .assertThat()
                                    .statusCode(http_code)
                                    .body('error', equalTo(error_message.toString()))

                        break
            }

        where:
            title << [
                    'Resume already active Customer Account',
                    'Resume terminated Customer Account',
                    'Terminate already defunct Customer Account',
                    'Change tenant-id parameter',
                    'Change customer-id parameter',
                    'Try to null status parameter',
                    "Try to set wrong status value (neither 'active' nor 'suspended')"]
            //"Unrecognized field \\\"customer-id\\\" "
            //"Unrecognized field \\\"tenant-id\\\" "
            ca_id 	  	|  map_param 				    | error_message	 													        || http_code
            customer_A	| ['status': "active"]		 	| "Cannot resume, customer account $customer_A is active" 			        || 422
            customer_B	| ['status': "active"]		 	| "Cannot resume, customer account $customer_B is terminated"   		    || 422
            customer_B	| _							 	| "Cannot terminate, customer account $customer_B is already terminated"    || 422
            customer_A	| ['tenant-id': "RU" ]		 	| "Unrecognized field \\\"tenant-id\\\""  			    		            || 400
            customer_A	| ['customer-id': "pnefedov_XX"]| "Unrecognized field \\\"customer-id\\\""							        || 400
            customer_A	| ['status': ""]		 		| "Wrong status"											   		        || 422
            customer_A	| ['status': "inactive"]		| "Wrong status"											  		        || 422

    }


}
