package com.jcat.cloud.fw.fwservices.monitoring.util;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 *
 * This class wraps the Hibernate services to simplify transactions.
 * It is used to insert Hibernate mapped Java POJOs to a database.
 * It tries to reuse sessions bound to the thread context, meaning
 * that the sessions are automatically closed when done.
 *
 * Hibernate is configured in the XML file located in the resource
 * folder, src/main/resources/hibernate.cfg.xml.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ehkiyki 2015-06-26 Initial version
 * @author ethssce 2015-08-17 adjusted to namespace structure
 */
public class DatabaseHelper {

    // The database transaction session factory
    private SessionFactory mSessionFactory;

    private static Logger mLogger = Logger.getLogger(DatabaseHelper.class);

    /**
     * The constructor
     */
    public DatabaseHelper() {
    }

    public static boolean isDbAccessiable() {
        Configuration configuration = new Configuration().configure();
        String url = configuration.getProperty("hibernate.connection.url");
        String username = configuration.getProperty("hibernate.connection.username");
        String password = configuration.getProperty("hibernate.connection.password");
        try {
            DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            mLogger.warn(String.format(
                    "Could not establish database connection to url %s using user %s and password %s", url, username,
                    password));
            return false;
        }
        return true;
    }

    private Session getDbSession() {
        if (mSessionFactory == null) {
            initDbSessionFactory();
        }
        return mSessionFactory.getCurrentSession();
    }

    private void initDbSessionFactory() {
        Configuration configuration = new Configuration().configure();
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration
                .getProperties());
        mSessionFactory = configuration.buildSessionFactory(builder.build());
    }

    /**
     * Gets a session if there is one, or otherwise creates one.
     * Starts a transaction and tries to commit the given objects.
     * It will rollback if the transaction fails.
     * @param objs List of objects to be committed.
     */
    public void save(List<? extends Object> objs) {
        Session session = getDbSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            for (Object obj : objs) {
                session.save(obj);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a session if there is one, or otherwise creates one.
     * Starts a transaction and tries to commit the given object.
     * It will rollback if the transaction fails.
     * @param obj Object to be committed.
     */
    public void save(Object obj) {
        Session session = getDbSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.save(obj);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
                e.printStackTrace();
            }
        }
    }
}
