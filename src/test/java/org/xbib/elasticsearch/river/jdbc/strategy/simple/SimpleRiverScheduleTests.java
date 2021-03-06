
package org.xbib.elasticsearch.river.jdbc.strategy.simple;

import org.elasticsearch.client.Client;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xbib.elasticsearch.river.jdbc.JDBCRiver;
import org.xbib.elasticsearch.river.jdbc.RiverSource;
import org.xbib.elasticsearch.river.jdbc.support.AbstractRiverNodeTest;
import org.xbib.elasticsearch.river.jdbc.support.RiverContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class SimpleRiverScheduleTests extends AbstractRiverNodeTest {

    @Override
    public RiverSource getRiverSource() {
        return new SimpleRiverSource();
    }

    @Override
    public RiverContext getRiverContext() {
        return new RiverContext();
    }

    /**
     * Product table star select, scheduled for more than one run
     */
    @Test
    @Parameters({"river6", "sql1"})
    public void testSchedule(String riverResource, String sql) throws Exception {
        Connection connection = source.connectionForWriting();
        createRandomProducts(connection, sql, 100);
        source.closeWriting();
        Client client = client("1");
        RiverSettings settings = riverSettings(riverResource);
        JDBCRiver river = new JDBCRiver(new RiverName(INDEX, TYPE), settings, client);
        river.start();
        Thread.sleep(12500L); // let the river run at least twice
        assertThat(client.prepareSearch(INDEX).execute().actionGet().getHits().getTotalHits(),
            greaterThan(104L));
        river.close();
    }

    private void createRandomProducts(Connection connection, String sql, int size)
            throws SQLException {
        for (int i = 0; i < size; i++) {
            long amount = Math.round(Math.random() * 1000);
            double price = (Math.random() * 10000) / 100.00;
            addData(connection, sql, UUID.randomUUID().toString().substring(0, 32), amount, price);
        }
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    private void addData(Connection connection, String sql, final String name, final long amount, final double price)
            throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        List<Object> params = new ArrayList<Object>() {{
            add(name);
            add(amount);
            add(price);
        }};
        source.bind(stmt, params);
        stmt.execute();
    }

}
