package ar.gmf;


import java.io.Serializable;
import java.io.Writer;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.shareddata.AsyncMap;

public class TestVerticle extends AbstractVerticle implements Serializable {

	private static final long serialVersionUID = 4264536910134665055L;

	protected transient final Logger logger = LoggerFactory.getLogger(getClass());

	transient XStream x = new XStream(new XppDriver() {
		@Override
		public HierarchicalStreamWriter createWriter(Writer out) {
			return new CompactWriter(out, getNameCoder());
		}
	});

	/*
	 * session (very needed of course!)
	 */
	String session;

	/*
	 * Verticle "session" variables
	 */
	Integer counter = 0;
	Date startDate;

	@Override
	public void start() throws Exception {
		super.start();

		startDate = new Date();

		x.omitField(AbstractVerticle.class, "context");
		x.omitField(AbstractVerticle.class, "vertx");

		session = config().getString("session");

		vertx.sharedData().getClusterWideMap("VM", r -> {
			if (r.succeeded()) {

				vertx.eventBus().consumer("emigrate-v-" + deploymentID(), msg -> {
					logger.info("undeploy " + "emigrate-v-" + deploymentID());
					vertx.undeploy(deploymentID(), h -> msg.reply("ok"));
				});

				AsyncMap<Object, Object> asyncMap = r.result();

				asyncMap.get(session, rr -> {
					if (rr.succeeded() && rr.result() != null) {
						logger.info("recovering TestVerticle desde sesion {}", deploymentID());

						XStream x = new XStream();
						x.fromXML((String) rr.result(), this);
					}
				});

				vertx.eventBus().consumer("click-"+session, msg -> {
					counter++;
					logger.info("clicked {} {} ", counter, session);

					String xml = x.toXML(TestVerticle.this);
					asyncMap.put(session, xml, h -> {
						msg.reply(String.format("Verticle started at %s the counter is %s", startDate, counter));
					});
				});

				vertx.eventBus().consumer("undeployClick-" + session, msg -> {
					logger.info("undeployClick at counter {} {}", counter, deploymentID());
					vertx.undeploy(deploymentID(), h -> {
						logger.info("undeployClick finished {}", h.succeeded());
						msg.reply("ok");
					});
				});

			} else {
				logger.error("error", r.cause());
			}
		});

	}

	@Override
	public void stop() throws Exception {
		super.stop();
		logger.info("ending TestVerticle {}", deploymentID());
	}

	public Integer getCounter() {
		return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}
}
