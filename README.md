# rabbitmq-rpc-example
> I built this example to better understand how to use RabbitMQ's RpcClient class in Java.
> There are two parts:
> 1. RpcClientTest
> 2. RpcServerTest
>
> In the RpcClientTest code, you can set 
> * THREADS: number of client threads
> * ITER: number of calls made by each thread
> * TIMEOUT_MS: how long to wait for reply before timing out
>
> Build the project using maven:
>      mvn compile
> then open a command-line, cd to the "target" folder of the project
> Start the server:

    $ java -cp example-1.0-shaded.jar RpcServerTest

> In a separate command-line, start the client:

    $ java -cp example-1.0-shaded.jar RpcClientTest

> At this point, you will see a pile of log messages in both windows.  Congratulations!
> Some things to note about this example:
> * If you run the client before the server, you will see timeout messages.
> * The number of server threads is not specified.  RabbitMQ defaults to 2x CPU.
> * The client doesn't need to synchronize use of RpcClient.
> * The server doesn't need to synchronize use of the channel for basicReply().
