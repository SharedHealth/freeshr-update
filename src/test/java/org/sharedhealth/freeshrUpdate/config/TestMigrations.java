package org.sharedhealth.freeshrUpdate.config;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.toddfast.mutagen.cassandra.CassandraMutagen;
import com.toddfast.mutagen.cassandra.CassandraSubject;
import com.toddfast.mutagen.cassandra.impl.CassandraMutagenImpl;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static java.lang.System.getenv;

public class TestMigrations {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestMigrations.class);

    private static final String MUTAGEN_CONNECTION_POOL_NAME = "shrMigrationConnectionPool";
    public static final int ONE_MINUTE = 6000;
    protected final Map<String, String> env;

    public TestMigrations(Map<String, String> env) {
        this.env = env;
    }

    public TestMigrations() {
        this(getenv());
    }


    public void migrate() throws IOException {
        String freeSHRKeyspace = env.get("CASSANDRA_KEYSPACE");
        Cluster cluster = connectKeyspace();
        Session session = createSession(cluster);
        CassandraMutagen mutagen = new CassandraMutagenImpl(freeSHRKeyspace);

        try {
            mutagen.initialize(env.get("CASSANDRA_MIGRATIONS_PATH"));
            com.toddfast.mutagen.Plan.Result<Integer> result = mutagen.mutate(new CassandraSubject(session,
                    freeSHRKeyspace));

            if (result.getException() != null) {
                throw new RuntimeException(result.getException());
            } else if (!result.isMutationComplete()) {
                throw new RuntimeException("Failed to apply cassandra migrations");
            }
        } finally {
            closeConnection(cluster, session);
        }
    }

    private Cluster connectKeyspace() {
        return connectCluster();
    }

    protected Cluster connectCluster() {
        Cluster.Builder clusterBuilder = new Cluster.Builder();

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.setConsistencyLevel(ConsistencyLevel.QUORUM);


        PoolingOptions poolingOptions = new PoolingOptions();

        clusterBuilder
                .withPort(Integer.parseInt(env.get("CASSANDRA_PORT")))
                .withClusterName(env.get("CASSANDRA_KEYSPACE"))
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withPoolingOptions(poolingOptions)
                .withProtocolVersion(Integer.parseInt(env.get("CASSANDRA_VERSION")))
                .withAuthProvider(new PlainTextAuthProvider(env.get("CASSANDRA_USER"), env.get("CASSANDRA_PASSWORD")))
                .withQueryOptions(queryOptions)
                .withReconnectionPolicy(new ConstantReconnectionPolicy(ONE_MINUTE))
                .addContactPoint(env.get("CASSANDRA_HOST"));
        return clusterBuilder.build();

    }

    protected Session createSession(Cluster cluster) {
        String keyspace = env.get("CASSANDRA_KEYSPACE");

        Session session = cluster.connect();
        session.execute(
                String.format(
                        "CREATE KEYSPACE  IF NOT EXISTS %s WITH replication = {'class':'SimpleStrategy', 'replication_factor':1}; ",
                        keyspace)
        );
        session.close();
        return cluster.connect(keyspace);
    }

    private void closeConnection(Cluster cluster, Session session) {
        session.close();
        cluster.close();
    }

    }
