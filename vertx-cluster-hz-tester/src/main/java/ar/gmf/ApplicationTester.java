package ar.gmf;


import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class ApplicationTester {

	final static Logger logger = LoggerFactory.getLogger(ApplicationTester.class);

	HazelcastInstance hazelcastInstance;
	Vertx vertx;

	public ApplicationTester() {
		Config config = new ClasspathXmlConfig("cluster.xml");
		hazelcastInstance = Hazelcast.newHazelcastInstance(config);

		Vertx.clusteredVertx(new VertxOptions().setClusterManager(new HazelcastClusterManager(hazelcastInstance)).setClustered(true).setHAEnabled(false), r -> {
			vertx = r.result();
			init();
		});
	}

	public static void main(String[] args) throws Throwable {
		new ApplicationTester();
	}

	private void init() {
		vertx.eventBus().send("getSession", "", r -> {
			if(r.succeeded()) {
				final String session = r.result().body().toString();
				vertx.setPeriodic(500l, h-> {
					Instant before = Instant.now();
					vertx.eventBus().send("click-"+session, "", rr -> {
						Instant after = Instant.now();
						Duration duration = Duration.between(before, after);
						String durationStr = StringUtils.remove(duration.toString(), "PT");
						if(rr.succeeded()) {
							logger.info("Response OK {} {} [{}]", session, rr.result().body(), durationStr);
						} else {
							logger.error("Response OK {} {} [{}]", session, rr.cause().getMessage(), durationStr);
						}
					});
				});
			}
		});
		
	}

}
