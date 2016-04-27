package ar.gmf;


import org.apache.commons.lang3.RandomStringUtils;
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

public class ApplicationHZ {

	final static Logger logger = LoggerFactory.getLogger(ApplicationHZ.class);

	HazelcastInstance hazelcastInstance;
	Vertx vertx;

	public ApplicationHZ() {
		Config config = new ClasspathXmlConfig("cluster.xml");
		hazelcastInstance = Hazelcast.newHazelcastInstance(config);

		Vertx.clusteredVertx(new VertxOptions().setClusterManager(new HazelcastClusterManager(hazelcastInstance)).setClustered(true).setHAEnabled(true), r -> {
			vertx = r.result();
			init();
		});
	}

	public static void main(String[] args) throws Throwable {
		new ApplicationHZ();
	}

	private void init() {
		String session = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
		vertx.eventBus().consumer("getSession", h -> h.reply(session));
		deployClickVerticle(session);
		
		logger.info("\n*******\n STARTED OK {} \n*******", session);
	}

	private void deployClickVerticle(String session) {
		DeploymentOptions options = new DeploymentOptions();
		options.setHa(true);

		JsonObject config = new JsonObject();
		config.put("session", session);
		options.setConfig(config);

		vertx.deployVerticle("ar.gmf.TestVerticle", options, h -> {
			if (h.succeeded()) {
				logger.info("deploy ClickVerticle {}", h.result());
			} else {
				logger.error("deploy ClickVerticle error {} ", h.cause());
			}
		});
	}

}
