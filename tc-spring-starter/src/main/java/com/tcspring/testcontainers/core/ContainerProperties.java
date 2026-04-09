package com.tcspring.testcontainers.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralized Docker image and credential configuration for all containers.
 *
 * <p>Override in {@code src/test/resources/application-test.properties}:
 * <pre>
 * tc.testcontainers.kafka.image=nexus.internal/kafka:3.8.0
 * tc.testcontainers.oracle.image=nexus.internal/oracle19c:slim
 * tc.testcontainers.ibm-mq.image=nexus.internal/ibm-mq:9.3.2
 * tc.testcontainers.redis.image=nexus.internal/redis:7
 * tc.testcontainers.sql-server.image=nexus.internal/mssql:2022
 * </pre>
 */
@ConfigurationProperties(prefix = "tc.testcontainers")
public class ContainerProperties {

    private final Kafka     kafka     = new Kafka();
    private final Oracle    oracle    = new Oracle();
    private final IbmMq     ibmMq     = new IbmMq();
    private final Redis     redis     = new Redis();
    private final SqlServer sqlServer = new SqlServer();

    public Kafka     getKafka()     { return kafka;     }
    public Oracle    getOracle()    { return oracle;    }
    public IbmMq     getIbmMq()    { return ibmMq;     }
    public Redis     getRedis()     { return redis;     }
    public SqlServer getSqlServer() { return sqlServer; }

    public static class Kafka {
        /** apache/kafka-native: KRaft, Kafka 3.8, GraalVM native, ~2s startup. */
        private String image = "apache/kafka-native:3.8.0";
        public String getImage()         { return image; }
        public void   setImage(String i) { this.image = i; }
    }

    public static class Oracle {
        /**
         * gvenzl/oracle-xe:21-slim-faststart: 21c XE, slim, pre-initialised.
         * No public Oracle 19c image exists — override with internal registry image.
         */
        private String image    = "gvenzl/oracle-xe:21-slim-faststart";
        private String username = "testuser";
        private String password = "testpass";
        private String database = "XEPDB1";
        public String getImage()            { return image;    }
        public String getUsername()         { return username; }
        public String getPassword()         { return password; }
        public String getDatabase()         { return database; }
        public void   setImage(String i)    { this.image = i;    }
        public void   setUsername(String u) { this.username = u; }
        public void   setPassword(String p) { this.password = p; }
        public void   setDatabase(String d) { this.database = d; }
    }

    public static class IbmMq {
        /** Free public image from IBM Container Registry — no login required. */
        private String image         = "icr.io/ibm-messaging/mq:latest";
        private String queueManager  = "QM1";
        private String appPassword   = "passw0rd1";
        private String adminPassword = "passw0rd1";
        private String channel       = "DEV.APP.SVRCONN";
        private String queue         = "DEV.QUEUE.1";
        public String getImage()                   { return image;         }
        public String getQueueManager()            { return queueManager;  }
        public String getAppPassword()             { return appPassword;   }
        public String getAdminPassword()           { return adminPassword; }
        public String getChannel()                 { return channel;       }
        public String getQueue()                   { return queue;         }
        public void   setImage(String i)           { this.image = i;           }
        public void   setQueueManager(String qm)   { this.queueManager = qm;   }
        public void   setAppPassword(String p)     { this.appPassword = p;     }
        public void   setAdminPassword(String p)   { this.adminPassword = p;   }
        public void   setChannel(String c)         { this.channel = c;         }
        public void   setQueue(String q)           { this.queue = q;           }
    }

    public static class Redis {
        /** redis:7-alpine: lightweight Redis for testing. */
        private String image = "redis:7-alpine";
        public String getImage()         { return image; }
        public void   setImage(String i) { this.image = i; }
    }

    public static class SqlServer {
        /** Official Microsoft SQL Server image for testing. */
        private String image    = "mcr.microsoft.com/mssql/server:2022-latest";
        private String password = "Str0ngPa$$w0rd";
        public String getImage()            { return image;    }
        public String getPassword()         { return password; }
        public void   setImage(String i)    { this.image = i;    }
        public void   setPassword(String p) { this.password = p; }
    }
}
