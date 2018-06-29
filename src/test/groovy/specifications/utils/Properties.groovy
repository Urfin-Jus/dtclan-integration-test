package specifications.utils

class Properties {

    //Environment dependant parameters

    //SVT - INTERNAL
    public static final mano_url = 'http://mano-global-dtag.deploy22.openshift.sdntest.netcracker.com'
    public static final user_name = 'superuser'
    public static final user_pswd = 'Superpassword_123'
    public static final natCo = 'RU'
    public static final tenant_Primary = 'QA-1'
    public static final tenant_Secondary = 'QA-1R'
    public static final fqdn_base = '1vcpe.com.'

    public static final client_id = '3b6773cb-0799-4b41-bc1c-70e9da960f21'
    public static final client_secret = 'AK16IVpb4_pgKmcx8N8zAEGgY781luI-w-pwBjXvSEqmNZZHuZ32rPwiZdTnIqSPqlWW4V3Lgg-NVYo-GMWDmCg'

    //HARDENING - EXTERNAL
    //public static final mano_url = 'https://mano-global-dtag.paas2.hard.cloud'
    //public static final natCo = 'HR001'
    //public static final tenant_Primary = 'ccpe_hr_fdc1'
    //public static final tenant_Secondary = 'ccpe_hr_fdc2'



    //Constant paths
    public static final account_endpoint = '/om/v1/customerAccount'
    public static final order_endpoint = '/om/v1/serviceOrder'
    public static final network_service_endpoint_Primary = "/srs/nfvo/api/v1.1/nso/tenants/$tenant_Primary/ns/records"
    public static final network_service_endpoint_Secondary = "/srs/nfvo/api/v1.1/nso/tenants/$tenant_Secondary/ns/records"
    public static final vnf_record_endpoint = '/srs/nfvo/api/vnfm/inventory/'
    public static final metric_list_endpoint= '/dt/monitoring/v1/metrics/files'


    //JSON scheme
    public static final account_scheme = 'scheme/account-schema.json'
    public static final branch_so_scheme = 'scheme/branch-so-schema.json'

    //Templates
    public static final account_request = 'requests/requiredAccount.json'
    public static final branch_request_required = 'requests/requiredBranch.json'



}

