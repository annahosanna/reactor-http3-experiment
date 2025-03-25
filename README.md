# reactor-http3-experiment

Using Project Reactor for a simple HTTP/3 server (until Netty has http/3 ~ 4.2 and Vert.x ~ 5)

## Status
* HTTP/3 upgrade is not working - current theory is untrusted self signed cert.
* Rapid requests cause an exception
* Still do not know which token handler is in use

## Basics of HTTP/3

- QUIC (which http3 is on top of) and HTTP/3 have acutally been around for a while. There are a large number of drafts with different libraries supporting different draft versions. QUIC and HTTP/3 have been finalized and are no longer in draft. The issues:
- QUIC is an OSI layer above UDP and below HTTP 3 .(whereas HTTP 1.1/2 are on top of TCP directly). QUIC (by neccessity) exists in user space rather than kernel. Thus applications processes and threads will each have to add the overhead of the QUIC stack. QUIC brings with it many advatages but might result in more CPU usage.
- Does you browser support QUIC (and HTTP/3) - If so, are there command line options required to enable it. Chrome requires command line flags or the url `Chrome://flags`. Chrome only switches to HTTP/3 after 1000 requests. Firefox will try to use HTTP/3 for every request. Safari doesn't support HTTP/3 at the time of writing this README.
- Some browsers may try HTTP/3 and HTTP/2 in parellel for the first request, while others browsers may first use HTTP/2 to determine if there is an `Alt-Svc` header and then switch to HTTP/3 for the next request (In which case HTTP/3 does not start on the first request).
- Does your browser support the same versions of quic, and http3 as the server. (or only one of the drafts instead)
- Http3 is unlike Http 1.1/2 and uses QUIC "channels" for header and data "frames".
- QUIC frames can arrive out of order. TCP enforces order for other http protocols.
- Http3 is a stream therefore content-length and chunked encoding headers are not required.
- HTTP/3 is on top of QUIC, and QUIC is on top of UDP. Make sure your statefull firewall can handle it.
- Http3 requires QUIC. However, to QUIC, http3 is an upper layer (i.e. it doesn't care about the upper layer protocol). In the OSI model think about TCP/UDP as layer 4, QUIC as layer 5, and HTTP/3 as layer 6/7.
- An http3 server is not establishing a layer on tcp with quic functionality. An http3 server is estabishing a stream (aka channel) on top of quic. This may seem mindbending for people used to implementing protocols on tcp - especially since quic is in userspace running as a part of the application server. An http3 server just handles data on a quic channel.
- Breaking this down a bit farther it means one piece of the program focuses on creating quic on top of udp, and a different part of the program focuses on creating http3 on top of quic.
- Ports are not part of quic. Ports are a part of tcp/udp.
- If you actually want a client to use http3, make sure your Http 1.1/2 server returns the Alt-Svc header
- HTTTP3 Does not use chuncked encoding and the Content-Length header is optional
- HTTP3 is based on streams and uses HEADER and DATA frames. Frames can arrive out of order. (which leads to the question what if the data frame number long_max + 1 is sent)

## Client - Server interaction Basics

- From the http/3 rfc - regarding the client: To the client everything appears to the same. The URI (including the URL, scheme, host and port), and same x509 certificate must also be used (Similar to CNAMES).
- My conclusions about this:
  1. The only way the scheme would not change is if http 1.1/2 were already using https, otherwise upgrade it to https first.
  2. The HTTP3 server must serve the same endpoint and urls as the http 1.1/2 server
  3. There are a few ways to request an upgrade to https. An easy solution would be to use a 301 redirect; however, this is often blocked by browsers - instead return a 426.
  ```
  Strict-Transport-Security: max-age=<time>
  Upgrade-Insecure-Requests: 1
  ```
- Be sure to turn ALPN on. The ALPN protocol names are the same as those used in Alt-Svc
- The standard header `Alt-Svc` seems like it might not be supported by chrome
- `Alt-Svc` (RFC 7838) can have multiple values comma seperated and the hostname is optional.
- Example `Alt-Svc: h3="localhost:8443", h2c="localhost:8443"`
- Chrome specific header: `Application-Protocol: h3,quic,h2,http/1.1`
- Here is a catch all: `Alt-Svc: h3=":443"; ma=86400, h3-29=":443"; ma=86400, h3-Q050=":443"; ma=86400, h3-Q046=":443"; ma=86400, h3-Q043=":443"; ma=86400, quic=":443"; ma=86400; v="43,46"`
- Possible workflow: Client connects to port 80 and receives a 426 to https port 443. When they connet to `https://hostname:443/` the response then includes the `Alt-Svc` header `h3=hostname:443`, where hostname must be the same as what is on the certificate. (subject alternative name). Both tcp:443 (traditional https) and udp:443 (quic) must use the same certificate.

## Applications

Web browsers have different ways of enabling http3

1. Firefox

- Enabled by default

2. Chrome (Desktop not mobile)

- Use `chrome://flags` and enable QUIC
- Starts using QUIC after 1000 connections

3. Edge?
4. Safari

- No

5. Opera?

## EXamples

- Project Reactor has good examples in source tree
