/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * VoltDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VoltDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CatalogUtilForClient {
    /**
     * Serialize a file into bytes. Used to serialize catalog and deployment
     * file for UpdateApplicationCatalog on the client.
     *
     * @param path
     * @return a byte array of the file
     * @throws IOException
     *             If there are errors reading the file
     */
    public static byte[] toBytes(File path) throws IOException {
        FileInputStream fin = new FileInputStream(path);
        byte[] buffer = new byte[(int) fin.getChannel().size()];
        try {
            if (fin.read(buffer) == -1) {
                throw new IOException("File " + path.getAbsolutePath() + " is empty");
            }
        } finally {
            fin.close();
        }
        return buffer;
    }
}
