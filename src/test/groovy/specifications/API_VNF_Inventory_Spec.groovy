package specifications

import spock.lang.Unroll

import static specifications.utils.Properties.account_endpoint
import static specifications.utils.Properties.natCo

import static specifications.utils.Properties.network_service_endpoint_Primary
import static specifications.utils.Properties.network_service_endpoint_Secondary

import groovy.json.JsonSlurper

import static io.restassured.RestAssured.*
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification

import static specifications.utils.Alfred.*

import spock.lang.*

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath


class API_VNF_Inventory_Spec extends Specification {

    @Shared def testRunID
    @Shared def prefix

    @Shared String inventory_account_id
    @Shared String inventory_order_id
    @Shared String inventory_branch_id

    @Shared String access_token
    //BRANCH NS
    @Shared String prim_branch_ns_key
    @Shared String second_branch_ns_key

    //Primary VNF Records
    @Shared String prim_ipfe_key
    @Shared String prim_nat_key
    @Shared String prim_sfc_key
    @Shared String prim_vsrx_key

    //Secondary VNF Records
    @Shared String second_ipfe_key
    @Shared String second_nat_key
    @Shared String second_sfc_key
    @Shared String second_vsrx_key



    @Shared RequestSpecification  requestSpec
    @Shared ResponseSpecification responseSpec_order
    @Shared ResponseSpecification responseSpec_account


    def setupSpec(){
        //Setup Config
        setPollingDefaults()
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        responseSpec_account = setupAccountResponseSpec()
        responseSpec_order = setupOrderResponseSpec()

        //Setup SpecProperties
        testRunID = new Date().format('MMddhhmm')
        prefix = "API-INVENTORY-$testRunID"
        inventory_account_id = ("$prefix-ACCOUNT_A").toString()
        inventory_order_id = ("$prefix-ORDER-$testRunID").toString()
        inventory_branch_id  = ("$prefix-BRANCH-INSTANCE-$testRunID").toString()

        /*//Prepare Request JSONs

        def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredAccount.json').toURI() )
        def json_body = new JsonSlurper().parse(template_file)
        def branch_template_file = new File ( getClass().getClassLoader().getResource('requests/optionalBranch.json').toURI() )
        def branch_json_body = new JsonSlurper().parse(branch_template_file)

        json_body.'customer-id' = inventory_account_id
        json_body.'admin-id' = ("$prefix-ADMIN_A").toString()
        json_body.status = "active"
        json_body.'tenant-id' = natCo

        branch_json_body.externalId = inventory_order_id
        branch_json_body.relatedParty[0].id = inventory_account_id
        branch_json_body.orderItem[0].service.id = inventory_branch_id
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value =  ("BRANCH-ID-$testRunID").toString()
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = ("SITE-ID-$testRunID").toString()
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "antivirus" }.value = 'yes'
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "antispam" }.value = 'yes'
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "web-filtering" }.value = 'yes'
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "content-filtering" }.value = 'yes'
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "configurable-firewall" }.value = 'yes'
        branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "application-qos" }.value = 'yes'

        //Create Account
        given().spec(requestSpec)
                .basePath(account_endpoint)
                .body(json_body).log().all()
                .post().then().log().all()
                .assertThat().statusCode(201)
        //Get Account Information
        given().spec(requestSpec)
                .basePath(account_endpoint).log().headers()
                .get(inventory_account_id).then().log().all()
                .spec(responseSpec_account)


        //Create Branch Order
        given().spec(requestSpec)
                .body(branch_json_body).log().all()
                .post().then().log().all()
                .spec(responseSpec_order)

        //Wait till Branch Order Completion
        pollForOrderCompletion(requestSpec,inventory_order_id)*/

        reportHeader "<h4>Customer Account: $inventory_order_id </h4>"
        reportHeader "<h4>Branch Service Instance: $inventory_branch_id </h4>"
    }

    def "Get Customer Service Instances"(){
        setup:
        inventory_account_id = "API-INVENTORY-06190136-ACCOUNT_A"
        def response = getServiceInventory( requestSpec, inventory_account_id)
        prim_branch_ns_key = response.then().extract().path("get(0).serviceCharacteristic.find {it.name == 'ns-record-primary-id'}.value")
        second_branch_ns_key = response.then().extract().path("get(0).serviceCharacteristic.find {it.name == 'ns-record-secondary-id'}.value")

        expect:
             response.then().assertThat()
                      .body(matchesJsonSchemaInClasspath("scheme/branch-service-inventory-scheme.json"))
                      //.body('serviceSpecification',equalTo(('branch-ccpe' as List)))

        reportInfo "Primary Branch Network Service: $prim_branch_ns_key "
        reportInfo "Secondary Branch Network Service: $second_branch_ns_key "
    }



    def "Get Primary Network Service"(){
        setup:
            access_token = getToken()
            Response ns_instance = getNetworkService(access_token,network_service_endpoint_Primary ,prim_branch_ns_key)
            prim_ipfe_key = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'ipfe' }.virtualNetworkFunction.resourceId")
            prim_nat_key  = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'nat'  }.virtualNetworkFunction.resourceId")
            prim_sfc_key  = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'sfc'  }.virtualNetworkFunction.resourceId")
            prim_vsrx_key = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'vsrx' }.virtualNetworkFunction.resourceId")
        expect:
            ns_instance.then()
                    .assertThat()
                    .body(matchesJsonSchemaInClasspath("scheme/branch-ccpe-ns-scheme.json"))
                    //.body('key', equalTo(branch_ns_key))


        reportInfo "Primary IPFE VNF: $prim_ipfe_key "
        reportInfo "Primary NAT VNF: $prim_nat_key "
        reportInfo "Primary SFC VNF: $prim_sfc_key "
        reportInfo "Primary vSRX VNF: $prim_vsrx_key "

        reportInfo "Response: ${ns_instance.body()}"
    }


    def "Get Secondary Network Service"(){
        setup:
        access_token = getToken()
        Response ns_instance = getNetworkService(access_token,network_service_endpoint_Secondary,second_branch_ns_key)
        second_ipfe_key = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'ipfe' }.virtualNetworkFunction.resourceId")
        second_nat_key  = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'nat'  }.virtualNetworkFunction.resourceId")
        second_sfc_key  = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'sfc'  }.virtualNetworkFunction.resourceId")
        //second_vsrx_key = ns_instance.jsonPath().get("constituentVnfRecords.find {vnf -> vnf.key == 'vsrx' }.virtualNetworkFunction.resourceId")
        expect:
        ns_instance.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath("scheme/branch-ccpe-ns-scheme.json"))
        //.body('key', equalTo(branch_ns_key))


        reportInfo "Primary IPFE VNF: $second_ipfe_key "
        reportInfo "Primary NAT VNF: $second_nat_key "
        reportInfo "Primary SFC VNF: $second_sfc_key "
        reportInfo "Primary vSRX VNF: $second_vsrx_key "

        reportInfo "Response: ${ns_instance.body()}"
    }


    @Unroll
    def "Get #focus_vnf VNF Record"(){

        setup:
            Response vnf_record = getVNFrecord(access_token, vnf_key)

        expect:
            vnf_record.then()
                    .assertThat()
                    .body(matchesJsonSchemaInClasspath(focus_schema))
                    //.body("name",startsWith('IPFE VNF Record'))

        reportInfo "Response: ${vnf_record.body()}"

        where:

        focus_schema = 'scheme/vnf-record-scheme.json'

        focus_vnf             | vnf_key          //| Specific parameters
        'Primary IPFE'        | prim_ipfe_key    //|
        'Primary NAT'         | prim_nat_key     //|
        'Primary SFC'         | prim_sfc_key     //|
        'Primary vSRX'        | prim_vsrx_key    //|
        'Secondary IPFE'      | second_ipfe_key  //|
        'Secondary NAT'       | second_nat_key   //|
        'Secondary SFC'       | second_sfc_key   //|
        //'Secondary vSRX'      | second_vsrx_key  //|


    }

    def cleanupSpec(){

        //Terminate Customer Account
        /*given().spec(requestSpec)
                    .basePath(account_endpoint)
                    .delete(inventory_account_id)
                    .then().log().all()
                    .statusCode(204)*/

        //Check services

    }


}

