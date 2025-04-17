# Building

### This is really simple:

- Certificates

1. Either obtain an x509 v3 certificate or create a self signed certificate.
2. The SAN needs to include the hostname of this server/workstation
3. The key should be named `key.pem` and the x509 certificate chain should be named `certs.pem`

- Build the server:

1. Ensure JAVA_HOME is set. This value depends on your platform and installation method. (In this case I'm using Amazon Corretto)
2. Ensure Gradle is installed (In this case I am using Homebrew)
3. gradle build
4. java -jar ./build/libs/http3-test.jar

- Testing (new terminal window)

1. test3.sh is used to test the 500,000 parallel requests with curl. (One connection takes 20 seconds)
2. ./make-test.sh
3. ./test3.sh

- Other Tests

1. test.sh attemps to open a whole bunch of connections (you will probably find you can't do more than about 500 per second)
2. test2.sh invokes a few instances of test3.sh concurrently. The performance from this is closely tied to efficient database access.
3. Database performance may drop off after several million records have been added. I am not sure if this is related to H2 or memory management; however, after some point, every time test2.sh is executed performance will decrease slightly.
