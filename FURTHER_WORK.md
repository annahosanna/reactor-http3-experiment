# Further work

## To Do

- Make this a full stack application by adding a single page web app as the user interface.
  - This is a pretty simple api. How can an already lightweight API be improved by an SPA?
    - Remove server side rendering:
      - Currently only the form content is rendered.
    - Make form data more robust
      - Passing data in JSON format removes type ambiguity.
        - Which is to say JSON can represent the value of `""` != null; `"1"` != `1`
    - Placing static content in the SPA removes the need to cache it on the server side.
    - Types of static content could be:
      - Images
        - Binary images can be cached in a CDN
        - Static SVG can be a part of an SPA or cached in a CDN
          - Dynamic images can be generated as SVGs - rather than as binary images generated by middleware
      - Stylesheets/Structured page rendering
      - Scripts (At least server side value calculations)
- Implement caching
  - How to efficently store static data which will be returned, without reading it from the filesystem every time
    - Cache it on client side or in a CDN

## Maybe To Do

- Implement authentication and authorization for secure access.
  - Do not have an SSO server like keycloak set up.

## Will Not Do (No relevant or out of scope)

- Implement data backup and recovery for disaster recovery.
  - Out of scope
- Implement rate limiting and throttling for security and performance.
  - Out of scope
- Implement continuous integration and deployment for automated testing and deployment.
  - Out of scope/Jenkins not set up
- Implement automated disaster recovery testing
  - Out of scope (but a good idea)
- Implement load balancing for improved performance.

## Done

- Added endpoints for Get (text/html) and Post (x-www-form-urlencoded)
- Added a added REST endpoints for Get (application/json) and Put (application/json)
- Optimized performance and scalability for high traffic.
- Implemented monitoring and logging for better insights.
- Implemented security measures such as input validation and encryption
