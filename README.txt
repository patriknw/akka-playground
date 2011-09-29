
This example illustrates that you should be careful with blocking and don’t be afraid of creating temporary actors that continue the message flow.

It is a translation service that deliver do two things as response to HTTP requests.
- translated text
- word count

The translation takes 100 ms to perform, and so does the word count.

We are only allowed to use 8 threads for the actors.

The application must be able to handle 4 concurrent client requests with an average response time of less than 110 ms.

web-to-backend in master is one way of solving this exercise with plain actors and one-way messages.
The blocking ask solution is tagged with 'blocking'.

How to run:
cd web-to-backend
sbt
> run-main akka.kernel.Main

Try with browser: http://localhost:9898/translate?text=Scala%20is%20great

Try with JMeter. Test script in web-to-backend/jmeter/jmeter-test.jmx


