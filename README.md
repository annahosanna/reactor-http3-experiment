# reactor-http3-experiment

## Status: Http/3 with H2 integration is working and has been tested with Firefox

## Summary

- I have put a lot of hours into doing things incorrectly - a lot of this was related to how flaky getting data is after subscribing. I'm pretty sure that more than once I spent a very long time trying to figure out why I wasn't getting any data, and my attempts to redo it probably did work - but there was no output. Furthermore working with POSTs was a pain and I ended up writing my own decoder. Finally it seemed like the pipeline was very fragile. To work around this I placed code (which would return a void) into a Filter. (Ideally I would have like a method that took Mono<T> as input and returned Monot<T> as output)
- POST workflow is to get the form parameters into a `Mono<String>` then to split those encoded form parameters into a `Flux<String>` where each parameter is decoded, then recombine them into `Mono<String>` as structured data of the deserialized JSON form of `List<Map<String,String>>` for easy processing.
- The GET routes and starting non blocking servers are mostly derived from the examples below. The most significant thing is handling form POSTs which is original work (and a lot of research). This readme is original work. (I have included two implementations to process POSTs)
- This program creates HTTP/1.1, HTTP/2, and HTTP/3 servers. Each server in turn produces headers to encourage the browser to switch to HTTPS and HTTP/3. (Set ma for h2 to 1 sec)
- You can test the latency yourself, but Http/3 appeared to be faster. Perhaps sometime I can set up Jmeter for accurate results.
- This project is only temporary, and I will switch to using Vert.x when Netty supports HTTP/3 (hopefully release 4.2), and Vert.x adds HTTP/3 support (hopefully Vert.x 5)
- The project has satisfied the goal of supporting http/3 and identifying implemenation issues with browsers and servers; and simulating an end to end workflow by reading and writing data to a database

## Testing Notes

1. HTTP/3 and POSTs are working - There are other libraries which support Http/3; however, all of the underlying handlers had to be created. After spending weeks researching that implementation, it only took a very short time to get a working http/3 server with this library. The benifit of that experience is that I had to have a much better understanding of QUIC and HTTP/3.

- Firefox has settings to enable http3 with self signed certs. (see below)
- Chrome keeps triggering errors on the server (Unless WebDeveloper Transport Tools are enable, in which case it doesn't even try http/3).
- For Chrome: Do not use "localhost" or "127.0.0.1" in your URL, but the actual hostname (Your software should not bind to localhost either)
- Certificates must be x509v3, have an extened key usage of serverAuth (especially for macos), and have the hostname or wildcard in the SAN. Note that wildcards can only be for subdomains - so if you have a self signed certificate you cannot have `*.local` as a SAN.
- QUIC is espeially sensitive to valid certificates, and will silently fail to negotiate - even in developer mode browsers do not report HTTP/3 connection attempts
- Oddly Firefox requests favicon.ico with evey http3 request, but not with http2. (see below for how to prevent)

2. Server error `javax.net.ssl.SSLHandshakeException: Received fatal alert: certificate_unknown` when ByteToMessageDecoder tries to decode the ssl stream. (see below)

- Chrome triggers an ssl error when requesting a page if a certificate is not trusted (such as the first visit to the site, using the refresh button, or when the Alt-Svc h2 ma expires) but not by following a link on the site; however, despite the server error, the correct content is returned via http/2 instead.

3. Still do not know which QUIC token handler is in use (Reactor QUIC includes an insecure token handler, so I do not know if a secure token handler is in use).
4. I do not know if a missing page causes the browser to fall back to http2; therefore, since favicon is always silently requested, I have accounted for that by returning an empty svg. Adding `link` to the header as in `<!DOCTYPE html><html><head><link rel="icon" href="data:,"/></head><body></body></html>` also prevents it from loading.

## Basics of HTTP/3

- QUIC (which http3 is on top of) and HTTP/3 have acutally been around for a while. There were a large number of drafts of each with different libraries supporting different draft versions. QUIC and HTTP/3 have been finalized and are no longer in draft.
- QUIC is an OSI layer above UDP and below HTTP 3 .(whereas HTTP 1.1/2 are on top of TCP directly).
- QUIC (by neccessity) exists in user space rather than kernel. While QUIC adds an extra layer, the tls negotiation happens once for http2, and once for http3, furthermore the http3 server must be added to an event loop, or as another thread, or as a new process.
- Because QUIC is reimplemented for every application, two applications that should do the same thing may handle QUIC differently. Which is unlike tcp/udp which is only implemented once (i.e. in the kernel). So conformity tests should include QUIC.
- Does your browser support QUIC (and HTTP/3) - If so, are there command line options required to enable it.(See below)
- Some browsers may try HTTP/3 and HTTP/2 in parellel for the first request, while others browsers may first use HTTP/2 to determine if there is an `Alt-Svc` header and then switch to HTTP/3 for the next request (In which case HTTP/3 does not start on the first request).
- Per RFC, ALPN is required for http2 and http3 (for instance this allows you to serve http/1.1 and http/2 off of the same port)
- Does your browser support the same versions of quic, and http3 as the server? (or only one of the drafts instead)
- Http3 is unlike Http 1.1/2 and uses QUIC "channels" for header and data "frames".
- QUIC frames can arrive out of order. TCP enforces the order for other http protocols.
- Http3 requires QUIC. However, to QUIC, http3 is an upper layer (i.e. it doesn't care about the upper layer protocol). In the OSI model think about TCP/UDP as layer 4, QUIC as layer 5, and HTTP/3 as layer 6/7.
- An http3 server is not establishing a layer on tcp/udp with quic functionality. An http3 server is estabishing a stream (aka channel) on top of quic. This may seem mindbending for people used to implementing application servers on tcp - especially since quic is in userspace running as a part of the application server. An http3 server just handles data on a quic channel.
- Breaking this down a bit farther it means one piece of the program focuses on creating quic on top of udp, and a different part of the program focuses on creating http3 on top of quic.
- Ports are not part of quic. Ports are a part of tcp/udp.
- If you actually want a client to use http3, make sure your Http 1.1/2 server returns the Alt-Svc header
- Http3 is a stream therefore content-length and chunked encoding headers are not required (but content length is optional).

## Client - Server interaction Basics

- Make sure your statefull firewall can handle UDP, since HTTP/3 is on top of QUIC, and QUIC is on top of UDP.
- From the http/3 rfc - regarding the client: To the client everything appears to be the same. The URI (including the URL, scheme, host and port), and the same x509 certificate must also be used (Similar to CNAMES).
- QUIC supports SNI for virtual hosts.
- My conclusions about this:
  1. The only way the scheme would not change is if http 1.1/2 were already using https, otherwise upgrade it to https first.
  2. The HTTP3 server must serve the same endpoint and urls as the http 1.1/2 server
  3. There are a few ways to request an upgrade to https. An easy solution would be to use a 301 redirect. A 426 did not seem to work well.
- `Alt-Svc` (RFC 7838) can have multiple values comma seperated and the hostname is optional (but not an `*` wildcard).
- Example (max age defaults to 24 hours). Use http/3 and fall back to http/2; however check again when max age expires: `Alt-Svc: h3=":8443", h2=":8443"; ma=1`
- Here is a catch all: `Alt-Svc: h3=":443"; ma=86400, h3-29=":443"; ma=86400, h3-Q050=":443"; ma=86400, h3-Q046=":443"; ma=86400, h3-Q043=":443"; ma=86400, quic=":443"; ma=86400; v="43,46"`
- I am not really sure why Alt-Svc allows a hostname or port to be specified, since it needs to be the same as the HTTP/2 URL,
- Possible workflow: Client connects to port 80 and receives a redirect to https port 443. When it connets to `https://hostname:443/` the response then includes the `Alt-Svc` header `h3=":443"`, where hostname must be the same as one of the certificates subject alternative name. Both tcp:443 (traditional https) and udp:443 (quic) must use the same (valid) certificate.

## Question

- HTTP3 is based on streams and uses HEADER and DATA frames. Frames can arrive out of order, which leads to the question what if the data frame number long_max + 1 is sent
- What happens if frame numbers are repeated
- Reactor HttpServer accepts ALPN negotiation; however, the documentation states that not all JDK 8 distributions support ALPN.
- If there is a problem with the connection will browsers always fall back to http/2 or if there is an http/2 problem will it try http/3? (which could very easily be the case for roaming devices which may change ip address.)

## Applications

Web browsers have different ways of enabling http3
Test your browser at `https://quic.nginx.org/quic.html` which has provides js to make 3000 requests to unique URLs. (text/plain no data)

See: `https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Alt-Svc`

- Safari and other apple products requiring http3 seem to be waiting for macos to be updated with libcurl with http3 support
- Chrome://flags and then enable QUIC
- Chrome://certificate-manager to manage custom certificates. !! Chrome requires the certificate to be issued by a trusted root for HTTP/3 !! So while you can import a self signed p12 to the macos system keystore, and chrome tls negotiation will work, it still will not be a trusted root.
- Firefox about:config network.http.http3.enabled and set to true
- Also if you are using self signed certs, there are additional http3 firefox options which need to be enabled

## Examples

- `https://github.com/reactor/reactor-netty/tree/main/reactor-netty-examples/src/main/java/reactor/netty/examples/http`
- `https://projectreactor.io/docs/netty/release/reference/http-server.html`
- Reading the unit tests is also very helpful
