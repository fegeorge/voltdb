/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.TestCase;

import org.voltdb.VoltDB.Configuration;
import org.voltdb.benchmark.tpcc.TPCCProjectBuilder;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.compiler.VoltProjectBuilder;
import org.voltdb.utils.BuildDirectoryUtils;
import org.voltdb.utils.MiscUtils;

public class TestHSQLBackend extends TestCase {

    public void testAdHocEmptyQuery() throws Exception {
        TPCCProjectBuilder builder = new TPCCProjectBuilder();
        builder.addDefaultSchema();
        builder.addDefaultPartitioning();
        builder.addProcedures(org.voltdb.benchmark.tpcc.procedures.SelectAll.class);

        String testDir = BuildDirectoryUtils.getBuildDirectoryPath();
        String catalogJar = testDir + File.separator + "tpcchsql.jar";

        builder.compile(catalogJar, 1, 1, 0);
        final File jar = new File(catalogJar);
        jar.deleteOnExit();

        ServerThread server = new ServerThread(catalogJar, builder.getPathToDeployment(), BackendTarget.HSQLDB_BACKEND);
        server.start();
        server.waitForInitialization();

        // run the test
        ClientConfig config = new ClientConfig("program", "none");
        Client client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        // call the insert procedure
        VoltTable[] results = client.callProcedure("@AdHoc", "select * from WAREHOUSE").getResults();
        // check one table was returned
        assertTrue(results.length > 0);
        assertTrue(results[0].getRowCount() == 0);

        server.shutdown();
        server.join();
    }

    public void testDateInsertionAsLong() throws Exception {
        TPCCProjectBuilder builder = new TPCCProjectBuilder();
        builder.addDefaultSchema();
        builder.addDefaultPartitioning();
        System.out.println("\n\n\n STARTING STMT PROC ADD\n\n\n");
        builder.addStmtProcedure("InsertHistory", "INSERT INTO HISTORY VALUES (?, ?, ?, ?, ?, ?, ?, ?);", "HISTORY.H_W_ID: 4");

        String testDir = BuildDirectoryUtils.getBuildDirectoryPath();
        String catalogJar = testDir + File.separator + "tpcchsql.jar";
        final File jar = new File(catalogJar);
        jar.deleteOnExit();

        builder.compile(catalogJar, 1, 1, 0);
        String pathToDeployment = builder.getPathToDeployment();

        ServerThread server = new ServerThread(catalogJar, pathToDeployment, BackendTarget.HSQLDB_BACKEND);
        server.start();
        server.waitForInitialization();

        // run the test
        ClientConfig clientConfig = new ClientConfig("program", "none");
        Client client = ClientFactory.createClient(clientConfig);
        client.createConnection("localhost");

        // call the insert procedure
        VoltTable[] results = client.callProcedure("InsertHistory", 5, 5, 5, 5, 5, 100000L, 2.5, "nada").getResults();
        // check one table was returned
        assertTrue(results.length > 0);
        assertTrue(results[0].getRowCount() == 1);

        server.shutdown();
        server.join();
    }

    public void testAdHocDateInsertionAsLong() throws UnknownHostException, IOException, ProcCallException, InterruptedException {
        TPCCProjectBuilder builder = new TPCCProjectBuilder();
        builder.addDefaultSchema();
        builder.addDefaultPartitioning();
        builder.addProcedures(org.voltdb.benchmark.tpcc.procedures.SelectAll.class);

        String testDir = BuildDirectoryUtils.getBuildDirectoryPath();
        String catalogJar = testDir + File.separator + "tpcchsql.jar";
        final File jar = new File(catalogJar);
        jar.deleteOnExit();

        builder.compile(catalogJar, 1, 1, 0);

        ServerThread server = new ServerThread(catalogJar, builder.getPathToDeployment(), BackendTarget.HSQLDB_BACKEND);
        server.start();
        server.waitForInitialization();

        // run the test
        ClientConfig config = new ClientConfig("program", "none");
        Client client = ClientFactory.createClient(config);
        client.createConnection("localhost");

        // call the insert procedure
        VoltTable[] results = client.callProcedure("@AdHoc", "INSERT INTO HISTORY VALUES (5, 5, 5, 5, 5, 100000, 2.5, 'nada');").getResults();
        // check one table was returned
        assertTrue(results.length > 0);
        assertTrue(results[0].getRowCount() == 1);

        server.shutdown();
        server.join();
        client.close();
    }

    public void testVarbinary() throws Exception {
        String simpleSchema =
            "create table blah (" +
            "ival bigint default 0 not null, " +
            "b varbinary default null, " +
            "PRIMARY KEY(ival));";

        VoltProjectBuilder builder = new VoltProjectBuilder();
        builder.addLiteralSchema(simpleSchema);
        builder.addPartitionInfo("blah", "ival");
        builder.addStmtProcedure("Insert", "insert into blah values (?, ?);", null);
        boolean success = builder.compile(Configuration.getPathToCatalogForTest("hsqldbbin.jar"), 1, 1, 0);
        assertTrue(success);
        MiscUtils.copyFile(builder.getPathToDeployment(), Configuration.getPathToCatalogForTest("hsqldbbin.xml"));

        VoltDB.Configuration config = new VoltDB.Configuration();
        config.m_pathToCatalog = Configuration.getPathToCatalogForTest("hsqldbbin.jar");
        config.m_pathToDeployment = Configuration.getPathToCatalogForTest("hsqldbbin.xml");
        config.m_backend = BackendTarget.HSQLDB_BACKEND;
        config.m_noLoadLibVOLTDB = true;
        ServerThread localServer = new ServerThread(config);
        localServer.start();
        localServer.waitForInitialization();

        Client client = ClientFactory.createClient();
        client.createConnection("localhost");

        ClientResponse cr = client.callProcedure("Insert", 5, new byte[] { 'a' });
        assertTrue(cr.getStatus() == ClientResponse.SUCCESS);

        // stop execution
        VoltDB.instance().shutdown(localServer);
    }
}
