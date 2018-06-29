package specifications

import spock.lang.*
import static specifications.utils.Alfred.*
import groovy.json.JsonSlurper
import java.util.concurrent.atomic.AtomicInteger
import io.restassured.RestAssured
import static io.restassured.RestAssured.*
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification

import static org.hamcrest.Matchers.*

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath

import static specifications.utils.Properties.account_endpoint
import static specifications.utils.Properties.fqdn_base
import static specifications.utils.Properties.natCo


class API_Branch_Spec extends Specification {

    @Shared def testRunID;     @Shared def prefix ;  @Shared def counter

    @Shared def customer_A;    @Shared def admin_A
    @Shared def customer_B;    @Shared def admin_B

    @Shared RequestSpecification requestSpec
    @Shared ResponseSpecification responseSpec_order
    @Shared ResponseSpecification responseSpec_account

    @Shared branch_json
    @Shared known_branch_id

    def setupSpec(){
        setPollingDefaults()
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        responseSpec_account = setupAccountResponseSpec()
        responseSpec_order = setupOrderResponseSpec()

        testRunID = new Date().format('MMddhhmm')
        prefix = "API-BRANCH-$testRunID"
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

    @Unroll("Create Branch as Test Data")
    def "Create Branch. Positive. Mandatory parameters only"(){
        setup:
        def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredBranch.json').toURI() )
        def branch_json_body = new JsonSlurper().parse(template_file)

        def ext_id = ("$prefix-ORDER-$testRunID-$counter").toString()
        def service_id = ("BRANCH-INSTANCE-$testRunID-$counter").toString()
        def branch_id = ("BRANCH-ID-$testRunID-$counter").toString()
        def site_id = ("SITE-ID-$testRunID-${counter.getAndIncrement()}").toString()

            branch_json_body.externalId = ext_id
            branch_json_body.relatedParty[0].id = ca_id
            branch_json_body.state = "InProgress"
            branch_json_body.orderItem[0].action = "add"
            branch_json_body.orderItem[0].service.id = service_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value = branch_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value = subnet
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = gateway
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = site_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value = bandwidth_dw
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value = bandwidth_up

        //Keep Provisioned Branch Details
        branch_json = branch_json_body
        known_branch_id = service_id
        expect:
            given().spec(requestSpec)
                        .body(branch_json_body).log().all()
                        .post().then().log().all()
                        .assertThat()
                        .statusCode(201)
                        .body(matchesJsonSchemaInClasspath("scheme/branch-so-schema.json"))
        and:
                        pollForOrderCompletion(requestSpec,ext_id)

        where:

        ca_id          | subnet            |   gateway     | bandwidth_dw  | bandwidth_up
        customer_A     |"192.168.10.0/24"  | "192.168.10.1"|   "20"        |   "20"



    }


    @Unroll("Create Branch. Negative. Check if #focus_parameter is mandatory")
    def "Create Branch. Negative"(){
        setup:
        def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredBranch.json').toURI() )
        def branch_json_body = new JsonSlurper().parse(template_file)

        def ext_id = ("$prefix-ORDER-$testRunID-$counter").toString()
        def state = "InProgress"
        def ca_id = customer_A
        def action = "add"
        def service_id = ("BRANCH-INSTANCE-$testRunID-$counter").toString()
        def branch_id = ("BRANCH-ID-$testRunID-$counter").toString()
        def subnet =  "192.168.10.0/24"
        def gateway = "192.168.10.1"
        def site_id = ("SITE-ID-$testRunID-${counter.getAndIncrement()}").toString()
        def bandwidth_down = "20"
        def bandwidth_up   = "20"

        switch(focus_parameter){
            case "external-id"	: ext_id = ""     ; break
            case "state"        : state  = ""     ; break
            case "customer-id"  : ca_id = ""      ; break
            case "action"       : action = ""     ; break
            case "service-id"   : service_id = "" ; break
            case "branch-id"	: branch_id = ""  ; break
            case "subnet"       : subnet = ""     ; break
            case "gateway"      : gateway = ""    ; break
            case "session-login": site_id = ""    ; break
            case "bandwidth_down":bandwidth_down="" ; break
            case "bandwidth_up" : bandwidth_up = "" ; break
        }

            branch_json_body.externalId = ext_id
            branch_json_body.relatedParty[0].id = ca_id
            branch_json_body.state = state
            branch_json_body.orderItem[0].action = action
            branch_json_body.orderItem[0].service.id = service_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value = branch_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value = subnet
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = gateway
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = site_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value = bandwidth_down
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value = bandwidth_up

        expect:
            given().spec(requestSpec)
                        .body(branch_json_body).log().all()
                        .post().then().log().all()
                        .assertThat()
                        .statusCode(400)
                        .body('error', equalTo(error_message))

        where:

        focus_parameter	|| error_message
        "external-id"	|| "External ID for request must be specified"
        "customer-id"	|| "Customer account is not specified"
        "state"			|| "Request validation has been failed, check your request and send it again. Problem might be with state"
        "action"		|| "Request validation has been failed, check your request and send it again. Problem might be with orderItem.action"
        "branch-id"		|| "Branch name must be set"
        "subnet"    	|| "Customer Branch Net must be set"
        "gateway"    	|| "Default Gateway must be set"
        "session-login" || "Branch Session Login must be set."
        "bandwidth_down"|| "Branch Bandwidth must be set"
        "bandwidth_up"	|| "Branch Uplink Bandwidth must be set"
        //"service-id"    || "something"

    }


    def "Modify Branch. Positive. Adding optional parameters"(){
        setup: 'Update successfully provisioned Branch Json with additional parameters '
            def template_file = new File ( getClass().getClassLoader().getResource('requests/optionalBranch.json').toURI() )
            def branch_json_body = new JsonSlurper().parse(template_file)

            def ext_id = ("$prefix-ORDER-$testRunID-${counter.getAndIncrement()}").toString()
            def user_id = ("$prefix-$testRunID-USER_A").toString()

                branch_json_body.externalId = ext_id
                branch_json_body.relatedParty[0].id = branch_json.relatedParty[0].id
                branch_json_body.orderItem[0].service.id = branch_json.orderItem[0].service.id
                branch_json_body.orderItem[0].action = 'modify'
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value

                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ipv6" }.value = "2001:db8:a::/64"
                branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "user-id" }.value = user_id


        expect:
            given().spec(requestSpec)
                .body(branch_json_body).log().all()
                .post().then().log().all()
                .assertThat()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("scheme/branch-so-schema.json"))
        and:
                pollForOrderCompletion(requestSpec,ext_id)


    }

    @Unroll("Create Branch. Negative. Check if #focus_parameter is unique")
    def "Create Branch. Negative. Check uniqueness"(){
        setup:
        def template_file = new File ( getClass().getClassLoader().getResource('requests/requiredBranch.json').toURI() )
        def branch_json_body = new JsonSlurper().parse(template_file)

        def ext_id = ("$prefix-ORDER-$testRunID-$counter").toString()
        def service_id = ("BRANCH-INSTANCE-$testRunID-$counter").toString()
        def branch_id = ("BRANCH-ID-$testRunID-$counter").toString()
        def site_id = ("SITE-ID-$testRunID-${counter.getAndIncrement()}").toString()

        switch(focus_parameter){
            case "external-id"   : ext_id = branch_json.externalId
                break
            case "service-id"    : service_id = branch_json.orderItem[0].service.id
                break
            case "session-login" : site_id = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value
                break
            //case "FQDN" :          ext_id = ""     ; break
            //case "public-ip" :     ext_id = ""     ; break
        }


            branch_json_body.externalId = ext_id
            branch_json_body.relatedParty[0].id = ca_id
            branch_json_body.orderItem[0].service.id = service_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value = branch_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = site_id
            // it.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "fqdn" }.value = fqdn
            // it.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-public-ip" }.value = public_ip

        expect:
        given().spec(requestSpec)
                .body(branch_json_body).log().all()
                .post().then().log().all()
                .assertThat()
                .statusCode(400)
                .body('error', equalTo(error_message))


        where:
        ca_id = customer_A

        focus_parameter	|| error_message
        "external-id"	|| "There is already order with external Id = ${branch_json.externalId} in the system.".toString()
        "service-id"    || "Already have instance with identification: ${branch_json.orderItem[0].service.id}".toString()
        "session-login" || "Branch with Session Login ${branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value} already exists".toString()
        //"FQDN"          || "FQDN is not unique"
        //"public-ip"     || "Public IP is not unique"

    }


    @Unroll("Create/Modify Branch. Not Allowed Operation. #title")
    def "Create Branch. Not Allowed Operation"(){
        setup:
        def template_file = new File ( getClass().getClassLoader().getResource('requests/optionalBranch.json').toURI() )
        def branch_json_body = new JsonSlurper().parse(template_file)

        def ext_id = ("$prefix-ORDER-$testRunID-$counter").toString()
        def ca_id = customer_B
        def service_id = ("BRANCH-INSTANCE-$testRunID-$counter").toString()
        def branch_id = ("BRANCH-ID-$testRunID-$counter").toString()
        def site_id = ("SITE-ID-$testRunID-${counter.getAndIncrement()}").toString()
        def fqdn = ("$known_branch_id.$fqdn_base").toString()

            branch_json_body.externalId = ext_id
            branch_json_body.relatedParty[0].id = ca_id
            branch_json_body.orderItem[0].service.id = service_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "branch-id" }.value = branch_id
            branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = site_id

        switch(testID){
            case "1"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value  = "192.168.20.0/24"
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = "192.168.50.1"
                         break
            case "2"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value  = "192.168.20.0/24"
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = "192.168.20.20"
                         break
            case "3"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value  = "192168.20.0/24"
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = "192.168.20.20"
                         break
            case "4"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value  = "192.168.20.0/24"
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "default-gateway" }.value = "192.16820.20"
                         break
            case "5"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "customer-branch-net-ip" }.value  = "192.168.20.0/25"
                         break
            case "6"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "user-id" }.value = admin_A
                         break
            case "7"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "fqdn" }.value = 'SOME.WRONG.FQDN'
                         break
            case "8"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value = "10000"
                         break
            case "9"   : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value ="10000"
                         break
            case "10"  : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-bandwidth" }.value = "-20"
                         break
            case "11"  : branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "external-uplink-bandwidth" }.value = "-20"
                         break
            case "12"  : branch_json_body.orderItem[0].action = 'modify'
                         branch_json_body.orderItem[0].service.id = branch_json.orderItem[0].service.id
                         break
            case "13"  : branch_json_body.orderItem[0].action = 'modify'
                         branch_json_body.relatedParty[0].id = customer_A
                         branch_json_body.orderItem[0].service.id = branch_json.orderItem[0].service.id
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value = branch_json.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "session-login" }.value
                         branch_json_body.orderItem[0].service.serviceCharacteristic.find { param -> param.name == "fqdn" }.value = fqdn
                         break
        }

        expect:
        given().spec(requestSpec)
                .body(branch_json_body).log().all()
                .post().then().log().all()
                .assertThat()
                .statusCode(400)
                .body('error', equalTo(error_message))

        where:
        testID  | title                                                    | error_message
        "1"     | 'Subnet vs Gateway mismatch'                             | "Default Gateway is out of subnet 192.168.20.0/24"
        "2"     | 'Gateway IP within DHCP Range'                           | "Default Gateway is inside range for dynamic addresses"
        "3"     | 'Subnet IP format validation check'                      | "Invalid CIDR 192168.20.0/24 (IPv4 format)"
        "4"     | 'Gateway IP format validation check'                     | "Invalid CIDR 192.16820.20 (IPv4 format)"
        "5"     | '/25 mask is not supported'                              | "Customer Branch Net mask should be /24"
        "6"     | 'Wrong User (who is actually different account admin)'   | " "
        "7"     | 'Wrong FQDN'                                             | "FQDN doesn't match DNS Zone"
        "8"     | 'Attempt to set excessive value for DownLink bandwidth'  | "Bandwidth value '10000' exceeds NatCo limits"
        "9"     | 'Attempt to set excessive value for UpLink bandwidth'    | "Uplink Bandwidth value '10000' exceeds NatCo limits"
        "10"    | 'Attempt to set negative value for DownLink bandwidth'   | "Bandwidth ('-20') value must be positive"
        "11"    | 'Attempt to set negative value for UpLink bandwidth'     | "Uplink Bandwidth ('-20') value must be positive"
        "12"    | 'Customer vs Service Instance mismatch'                  | "Request validation has been failed, relatedParty.id and service.id are referring to different customers"
        "13"    | 'Update FQDN for already provisioned branch'             | "FQDN Update is forbidden"
    }


    def cleanupSpec(){

            given().spec(requestSpec)
                                .basePath(account_endpoint).log().all()
                                .delete("$customer_A").then().log().all()
                                .assertThat()
                                .statusCode(204)

            given().spec(requestSpec)
                                .basePath(account_endpoint).log().all()
                                .delete("$customer_B").then().log().all()
                                .assertThat()
                                .statusCode(204)
    }



}
