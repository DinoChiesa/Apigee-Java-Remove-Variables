# Apigee custom policy to remove variables

This directory contains the Java source code and pom.xml file required to
compile a Java callout for Apigee. The callout performs one simple job:
it removes variables.

## Building:

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache
  ```./buildsetup.sh```

2. Build with maven.
  ```mvn clean package```

3. if you edit proxy bundles offline, copy the resulting jar file, available in  target/apigee-callout-remove-variable-20230526.jar to your apiproxy/resources/java directory.  If you don't edit proxy bundles offline, upload the jar file into the API Proxy via the Apigee API Proxy Editor .

4. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:
   ```xml
   <JavaCallout name='Java-Remove-AE-Variables'>
     <Properties>
       <Property name="recurse-access-entity">true</Property>
       <Property name="variables">AccessEntity.AE-API-Product</Property>
     </Properties>
     <ClassName>com.google.apigee.callouts.RemoveVariable</ClassName>
     <ResourceURL>java://apigee-callout-remove-variable-20230526.jar</ResourceURL>
   </JavaCallout>
   ```

   * The `variables` value can be a comma-separated list.

   * The `recurse-access-entity` is a boolean, denoting special behavior for
     variables set with AccessEntity.  The policy will remove ALL the
     "sub-variables" when this property is true.

5. use the Google Cloud Console UI, or a tool like [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js-examples/blob/master/importAndDeploy.js) or similar to
   import the proxy into an Edge organization, and then deploy the proxy .
   Eg,
   ```
   TOKEN=`gcloud auth print-access-token`
   node ./importAndDeploy -v --token $TOKEN --apigeex -o $ORG -e $ENV  -d bundle
   ```

6. Start a trace in Apigee, then use a client to generate and send http requests to the proxy. Eg,
   ```
   curl -i https://my-endpoint.net/remove-variable-demo/t1
   curl -i https://my-endpoint.net/remove-variable-demo/t2
   ```
   In the trace window, you will see that

     - for the first request, the 2nd call to AccessEntity does not set variables.

     - for the 2nd request, the 2nd call to AccessEntity DOES set variables, because the
       Java callout has removed all the readonly variables.


## Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0


If you want to download them manually:

* The 2 jars are available in Apigee Edge. The first two are
  produced by Apigee; contact Apigee support to obtain these jars to allow
  the compile, or get them here:
  https://github.com/apigee/api-platform-samples/tree/master/doc-samples/java-cookbook/lib


## Bugs

There are no unit tests for this project.
