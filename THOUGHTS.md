# Some thoughts and opinions

### Regarding Reactor

- I have put a lot of hours into learning this. There was a lot of trial and error which probably worked but due to how flaky getting data from subscribing can be, I did not think my pipeline was working correctly. Its seems other people have had the same experience - difficulty identifying where problems are occuring in a pipeline.
- Working with POSTs via `receiveFrom()` was a pain and I ended up writing my own decoder. I think part of the reason was because examples were trivial one liners, and I wanted to actually do something. In the end I extended the solution I came up with to work for both.
- To my point above, it seemed like the pipeline was very fragile. (Stops working with a slight change or errors reported in one part of the pipeline are actually realted to a completely different part of the pipeline)
- I spent a lot of time researching other HTTP3 Netty implementations but Reactor was definitely the way to go. The benefit of that experience is that I have a much better understanding of QUIC and HTTP/3.
- Errors and Warnings: I was receiving various SSL negotiation errors and 'LEAK' errors from 'aggregate'. Once the pipeline was modified to filter asString(), then the errors went away. Traditionally if the system reported a problem occured in a method, then you would look in that method for the error; however, this is a good example of the issue being resolved by actions later in the pipeline.
- I should probably look for more opportunities to return `Mono<void>` and to not assume the datatype passed to a lambda.
