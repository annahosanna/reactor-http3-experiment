# reactor-http3-experiment

## Summary

- This uses a combination of the examples referenced below, so I do not want to take credit for the similarities between this and those examples. Differences: There was not a single example which included the combined functionality to start three non blocking servers each serving a diffirent http protocol, routes which were seperate from the http server, and return various content. This readme is original work.
- This program creates HTTP/1.1, HTTP/2, and HTTP/3 servers. Each server in turn produces headers to encourage the browser to switch to HTTPS and HTTP/3.
- This project is only temporary, and I will switch to using Vert.x when Netty supports HTTP/3 (hopefully release 4.2), and Vert.x adds HTTP/3 support (hopefully Vert.x 5)

## Status

- HTTP/3 upgrade is not working - current theory is untrusted self signed cert.
- Rapid requests cause an ssl exception - not sure why
- Still do not know which QUIC token handler is in use (for secure tokens).
- I do not know if a missing page causes the browser to fall back to http2; therefore, since favicon is always silently requested, I have accounted for that by returning an empty svg.

## Basics of HTTP/3

- QUIC (which http3 is on top of) and HTTP/3 have acutally been around for a while. There were a large number of drafts of each with different libraries supporting different draft versions. QUIC and HTTP/3 have been finalized and are no longer in draft.
- QUIC is an OSI layer above UDP and below HTTP 3 .(whereas HTTP 1.1/2 are on top of TCP directly).
- QUIC (by neccessity) exists in user space rather than kernel. Thus applications processes and threads will each have to add the overhead of the QUIC stack. QUIC brings with it many advatages but might result in either more CPU usage, or throughput having to deal with a GC that stops the world.
- Because QUIC is reimplemented for every application, two applications that should do the same thing may handle QUIC differently. Which is unlike tcp/udp which is only implemented once (i.e. in the kernel). So conformity tests should include QUIC.
- Does your browser support QUIC (and HTTP/3) - If so, are there command line options required to enable it.(See below)
- Some browsers may try HTTP/3 and HTTP/2 in parellel for the first request, while others browsers may first use HTTP/2 to determine if there is an `Alt-Svc` header and then switch to HTTP/3 for the next request (In which case HTTP/3 does not start on the first request).
- ALPN must be enabled on the server to negotiate the protocol.
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
- From the http/3 rfc - regarding the client: To the client everything appears to be the same. The URI (including the URL, scheme, host and port), and same x509 certificate must also be used (Similar to CNAMES).
- QUIC supports SNI for virtual hosts.
- My conclusions about this:
  1. The only way the scheme would not change is if http 1.1/2 were already using https, otherwise upgrade it to https first.
  2. The HTTP3 server must serve the same endpoint and urls as the http 1.1/2 server
  3. There are a few ways to request an upgrade to https. An easy solution would be to use a 301 redirect; however, this is often blocked by browsers. Maybe try to return a 426?
  ```
  Strict-Transport-Security: max-age=<time>
  Upgrade-Insecure-Requests: 1
  ```
- On the server, be sure to turn ALPN on. The ALPN protocol names are the same as those used in Alt-Svc (although this may be part of QUIC)
- `Alt-Svc` (RFC 7838) can have multiple values comma seperated and the hostname is optional (but not an `*` wildcard).
- Example `Alt-Svc: h3="localhost:8443", h2c="localhost:8443"`
- Here is a catch all: `Alt-Svc: h3=":443"; ma=86400, h3-29=":443"; ma=86400, h3-Q050=":443"; ma=86400, h3-Q046=":443"; ma=86400, h3-Q043=":443"; ma=86400, quic=":443"; ma=86400; v="43,46"`
- I am not really sure why Alt-Svc allows a hostname to be specified, since it needs to be the same as the HTTP/2 URL,
- Possible workflow: Client connects to port 80 and receives a redirect to https port 443. When it connets to `https://hostname:443/` the response then includes the `Alt-Svc` header `h3=":443"`, where hostname must be the same as one of the certificates subject alternative name. Both tcp:443 (traditional https) and udp:443 (quic) must use the same (valid) certificate.

## Question

- HTTP3 is based on streams and uses HEADER and DATA frames. Frames can arrive out of order, which leads to the question what if the data frame number long_max + 1 is sent
- What happens if frame numbers are repeated
- Reactor HttpServer does not provide a way to enable ALPN and notes in the documentation that not all JDK 8 distributions support ALPN. (So is it enabled for non JDK 8?)
- If there is a problem with the connection will browsers always fall back to http/2 or if there is an http/2 problem will it try http/3? (which could very easily be the case for roaming devices which may change ip address.)

## Applications

Web browsers have different ways of enabling http3
Test your browser at `https://quic.nginx.org/quic.html` which has provides js to make 3000 requests to unique URLs. (text/plain no data)

1. Firefox - Enabled by default
2. Chrome (Desktop not mobile) - Use `chrome://flags` and enable QUIC; Starts using QUIC after 1000 connections ()
3. Edge?
4. Safari - Not currently
5. Opera - ?
6. Curl - ?

## Examples

- `https://github.com/reactor/reactor-netty/tree/main/reactor-netty-examples/src/main/java/reactor/netty/examples/http`
- `https://projectreactor.io/docs/netty/release/reference/http-server.html`
