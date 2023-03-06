package com.example.hospital;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;

abstract class GrpcTestBase {

  private static final String TEST_HOST = "127.0.0.1";
  private static final int TEST_PORT = 30111;
  private static final Map<Integer, Server> serverMap = new HashMap<Integer, Server>();

  private static Server buildServer(BindableService bs, int offsetPort) {
    return ServerBuilder.forPort(TEST_PORT + offsetPort).addService(bs).build();
  }

  protected static ManagedChannel buildChannel(int offsetPort) {
    return ManagedChannelBuilder.forAddress(TEST_HOST, TEST_PORT + offsetPort)
        .usePlaintext()
        .build();
  }

  protected static ManagedChannel buildChannel() {
    return buildChannel(0);
  }

  protected static void shutdown(Server server) throws InterruptedException {
    server.shutdown().awaitTermination();
  }

  private static void setShutdown(Server server) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        shutdown(server);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }));
  }

  protected static void initServer(BindableService bs, int offsetPort, Runnable setBlockingStub) throws IOException {
    if (!serverMap.containsKey(offsetPort)) {
      val server = buildServer(bs, offsetPort).start();
      serverMap.put(offsetPort, server);

      setBlockingStub.run();
      setShutdown(server);
    }
  }

  protected static void initServer(BindableService bs, Runnable setBlockingStub) throws IOException {
    initServer(bs, 0, setBlockingStub);
  }

  @BeforeAll
  static void setLocale() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
  }
}
