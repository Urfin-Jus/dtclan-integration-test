package specifications

import spock.lang.*
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import groovy.json.JsonSlurper

import java.util.concurrent.atomic.AtomicInteger

import static io.restassured.RestAssured.given
import static io.restassured.RestAssured.given
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath
import static org.hamcrest.Matchers.equalTo
import static specifications.utils.Alfred.*
import static specifications.utils.Properties.account_endpoint
import static specifications.utils.Properties.natCo

class API_VPN_Spec extends Specification{

    @Shared def testRunID;     @Shared def prefix ;  @Shared def counter

    @Shared def customer_A;    @Shared def admin_A
    @Shared def customer_B;    @Shared def admin_B

    @Shared RequestSpecification requestSpec
    @Shared ResponseSpecification responseSpec_order
    @Shared ResponseSpecification responseSpec_account


    //Test Data Keepers
    @Shared vpn_json ; @Shared existing_external_id
    @Shared existing_service_id ; @Shared existing_vpn_id


    def setupSpec(){
        setPollingDefaults()
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        responseSpec_account = setupAccountResponseSpec()
        responseSpec_order = setupOrderResponseSpec()

        testRunID = new Date().format('MMddhhmm')
        prefix = "API-VPN-$testRunID"
        counter = new AtomicInteger()

        customer_A = ("$prefix-$testRunID-ACCOUNT_A").toString()
        customer_B = ("$prefix-$testRunID-ACCOUNT_B").toString()
        admin_A = ("$prefix-$testRunID-ADMIN_A").toString()
        admin_B = ("$prefix-$testRunID-ADMIN_B").toString()

    }

    @Unroll("Create Account as Test Data: #ca_id , #admin_id")
    def "Create Account. Positive. As Test Data"(){
        setup:

        def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredAccount.json').toURI() )
        def json_body = new JsonSlurper().parse(template_file)

        json_body.'customer-id' = ca_id.toString()
        json_body.'admin-id' = admin_id.toString()
        json_body.status = "active"
        json_body.'tenant-id' = tenant_id.toString()

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


    @Unroll("Create VPN. Negative. Check #focus_parameter request parameter is mandatory")
    def "Create VPN. Mandatory parameters check"(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredVPN.json').toURI() )
            def vpn_json_body = new JsonSlurper().parse(template_file)

            def ext_id = "$prefix-$testRunID-ORDER-${counter.getAndIncrement()}".toString()
            def service_id = "VPN-INSTANCE-$testRunID-$counter".toString()
            def vpn_id = "VPN-ID-$testRunID-$counter".toString()

            switch(focus_parameter){
                case "external_id"	: ext_id = "" ; break

                case "vpn_id"		: vpn_id = "" ; break
            }

            vpn_json_body.externalId = ext_id
            vpn_json_body.relatedParty[0].id = ca_id
            vpn_json_body.state = state
            vpn_json_body.orderItem[0].action = action
            vpn_json_body.orderItem[0].service.id = service_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "vpn-id" }.value = vpn_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "sites-quota" }.value = quota

        expect:
            given().spec(requestSpec)
                    .body(vpn_json_body).log().all()
                    .post().then().log().all()
                    .assertThat()
                    .statusCode(400)
                    .body('error', equalTo(error_message))

        where:

        focus_parameter	| state 		|   ca_id 	  		           | action | quota	    || error_message
        "external_id"	| "InProgress"	|	"vpn_api_test_account_#2A" | "add"  |	"3"	    || "External ID for request must be specified"
        "customer_id"	| "InProgress"	|	""				           | "add"  |	"3"	    || "Customer account is not specified"
        "state"			| ""			|	"vpn_api_test_account_#2A" | "add"  |	"3"	    || "Request validation has been failed, check your request and send it again. Problem might be with state"
        "action"		| "InProgress"	|	"vpn_api_test_account_#2A" | ""	    |	"3"	    || "Request validation has been failed, check your request and send it again. Problem might be with orderItem.action"
        "vpn_id"		| "InProgress"	|	"vpn_api_test_account_#2A" | "add"  |	"3"	    || "VPN ID must be set for VPN TS order"
        "site_quota"	| "InProgress"	|	"vpn_api_test_account_#2A" | "add"  |	""	    || "VPN Sites Limit must be set"
        //"service_id"	| "InProgress"	|	"vpnAPI-account_A" | "add"  |	"3"	    || "Already have instance with identification: "

    }


    def "Create VPN. Positive. As Test Data"(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredVPN.json').toURI() )
            def vpn_json_body = new JsonSlurper().parse(template_file)

            def ext_id = "$prefix-$testRunID-ORDER-$counter".toString()
            def service_id = "VPN-INSTANCE-$testRunID-$counter".toString()
            def vpn_id = "VPN-ID-$testRunID-${counter.getAndIncrement()}".toString()


            vpn_json_body.externalId = ext_id
            vpn_json_body.relatedParty[0].id = customer_A
            vpn_json_body.state = "InProgress"
            vpn_json_body.orderItem[0].action = "add"
            vpn_json_body.orderItem[0].service.id = service_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "vpn-id" }.value = vpn_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "sites-quota" }.value = '3'

            vpn_json = vpn_json_body
            existing_vpn_id = vpn_id
            existing_service_id = service_id
            existing_external_id = ext_id

        expect:
            given().spec(requestSpec)
                    .body(vpn_json_body).log().all()
                    .post().then().log().all()
                    .assertThat()
                    .statusCode(201)
                    //.body(matchesJsonSchemaInClasspath("scheme/branch-so-schema.json"))
        and:
            pollForOrderCompletion(requestSpec,ext_id)

    }


    @Unroll("Modify VPN. Not Allowed Operation: #title ")
    def "Modify. Not Allowed Operations "(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredVPN.json').toURI() )
            def vpn_json_body = new JsonSlurper().parse(template_file)

            def ext_id = "$prefix-$testRunID-ORDER-$counter".toString()
            def service_id = "VPN-INSTANCE-$testRunID-$counter".toString()
            def vpn_id = "VPN-ID-$testRunID-${counter.getAndIncrement()}".toString()

            vpn_json_body.externalId = ext_id
            vpn_json_body.relatedParty[0].id = ca_id
            vpn_json_body.state = state
            vpn_json_body.orderItem[0].action = action
            vpn_json_body.orderItem[0].service.id = service_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "vpn-id" }.value = vpn_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "sites-quota" }.value = quota

        expect:
            given().spec(requestSpec)
                    .body(vpn_json_body).log().all()
                    .post().then().log().all()
                    .assertThat()
                    .statusCode(400)
                    .body('error', equalTo(error_message))

        where:
        title << [
                'site-quota low boundary value check',
                'site-quota up boundary value check',
                'Create VPN with Cancelled status',
                'Attempt of 2nd VPN Service creation']

        http_code = "HTTP/1.1 400 Bad Request"

        state 		    |   ca_id 	  	| action	| quota	 || error_message
        "InProgress"	|	customer_B	| "add"	 	|	"1"	 || "VPN Sites Limit must be greater than 1"
        "InProgress"	|	customer_B	| "add"	 	|	"33" || "VPN Sites Limit should not exceed 32"
        "Cancelled"		|	customer_B	| "add"	 	|	"3"  || "Incorrect status. Status can be InProgress or Acknowledged"
        "InProgress"	|	customer_A	| "add"	 	|	"3"  || "Customer can have only one active VPN"

    }

    @Issue(" Characteristic \'vpn-id\' is not unique")
    @Unroll("Create VPN. Check #focus_parameter is unique")
    def "Create. Unique parameters check"(){
        setup:
            def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredVPN.json').toURI() )
            def vpn_json_body = new JsonSlurper().parse(template_file)

            def ext_id = "$prefix-$testRunID-ORDER-${counter.getAndIncrement()}".toString()
            def service_id = "VPN-INSTANCE-$testRunID-$counter".toString()
            def vpn_id = "VPN-ID-$testRunID-$counter".toString()


            switch(focus_parameter){
                case "vpn_id"		:   vpn_id = existing_vpn_id ; break

                case "service_id"	:   service_id = existing_service_id ; break

                case "external_id"	:   ext_id = existing_external_id ; break
            }

            vpn_json_body.externalId = ext_id
            vpn_json_body.relatedParty[0].id = customer_B
            vpn_json_body.state = 'InProgress'
            vpn_json_body.orderItem[0].action = 'add'
            vpn_json_body.orderItem[0].service.id = service_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "vpn-id" }.value = vpn_id
            vpn_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "sites-quota" }.value = '3'

        expect:
            given().spec(requestSpec)
                .body(vpn_json_body).log().all()
                .post().then().log().all()
                .assertThat()
                .statusCode(400)
                .body('error', equalTo(error_message.toString()))

        where:

            focus_parameter | error_message
            "external_id"   | "There is already order with external Id = $existing_external_id in the system."
            "service_id"    | "Already have instance with identification: $existing_service_id"
            "vpn_id"        | "Already have instance with identification: $existing_vpn_id"
    }

    def "Delete Account. Positive. All Accounts get deleted"(){
        expect:
           // wait(5000)
            given().spec(requestSpec)
                        .basePath(account_endpoint).log().all()
                        .delete(ca_id.toString()).then().log().all()
                        .assertThat()
                        .statusCode(204)
                        .body(matchesJsonSchemaInClasspath("scheme/account-schema.json"))

        where:
          ca_id  << [customer_A,customer_B]

    }


}
