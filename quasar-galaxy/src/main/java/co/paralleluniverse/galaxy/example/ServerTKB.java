/*
 * Galaxy
 * Copyright (C) 2012 Parallel Universe Software Co.
 * 
 * This file is part of Galaxy.
 *
 * Galaxy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * Galaxy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Galaxy. If not, see <http://www.gnu.org/licenses/>.
 */
package co.paralleluniverse.galaxy.example;

import co.paralleluniverse.galaxy.Server;
import java.net.URL;

/**
 *
 * @author pron
 */
public class ServerTKB {
    public static void main(String[] args) {
        final URL serverXml = ServerTKB.class.getClassLoader().getResource("config/server.xml");
        final URL serverProps = ServerTKB.class.getClassLoader().getResource("config/server.properties");
        Server.start(serverXml.getPath(), serverProps.getPath());
    }
}
