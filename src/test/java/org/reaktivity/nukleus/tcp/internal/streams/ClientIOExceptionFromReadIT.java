/**
 * Copyright 2016-2017 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.nukleus.tcp.internal.streams;

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.reaktivity.nukleus.tcp.internal.TcpCountersRule;
import org.reaktivity.reaktor.test.NukleusRule;

/**
 * Tests the handling of IOException thrown from SocketChannel.read (see issue #9). This condition  is forced
 * in this test by causing the remote end to send a TCP reset (RST) by setting SO_LINGER to 0 then closing the socket,
 * as documented in <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/net/articles/connection_release.html">
 * Orderly Versus Abortive Connection Release in Java</a>

 */
public class ClientIOExceptionFromReadIT
{
    private final K3poRule k3po = new K3poRule()
        .addScriptRoot("route", "org/reaktivity/specification/nukleus/tcp/control/route")
        .addScriptRoot("streams", "org/reaktivity/specification/nukleus/tcp/streams");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final NukleusRule nukleus = new NukleusRule("tcp")
        .directory("target/nukleus-itests")
        .commandBufferCapacity(1024)
        .responseBufferCapacity(1024)
        .counterValuesBufferCapacity(1024)
        .streams("tcp", "source#partition");

    private final TcpCountersRule counters = new TcpCountersRule()
        .directory("target/nukleus-itests")
        .commandBufferCapacity(1024)
        .responseBufferCapacity(1024)
        .counterValuesBufferCapacity(1024);

    @Rule
    public final TestRule chain = outerRule(SocketChannelHelper.RULE)
                                  .around(nukleus).around(counters).around(k3po).around(timeout);

    @Test
    @Specification({
        "${route}/output/new/controller",
        "${streams}/server.close/client/source"
    })
    public void shouldReportIOExceptionFromReadAsEndOfStream() throws Exception
    {
        try (ServerSocketChannel server = ServerSocketChannel.open())
        {
            server.setOption(SO_REUSEADDR, true);
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_OUTPUT");

            try (SocketChannel channel = server.accept())
            {
                k3po.notifyBarrier("ROUTED_INPUT");

                channel.setOption(StandardSocketOptions.SO_LINGER, 0);
                channel.close();

                k3po.finish();
            }
        }
    }

    @Test
    @Specification({
        "${route}/output/new/controller",
        "${streams}/client.sent.data.received.reset/client/source"
    })
    public void writeAfterIOExceptionFromReadShouldResultInReset() throws Exception
    {
        try (ServerSocketChannel server = ServerSocketChannel.open())
        {
            server.setOption(SO_REUSEADDR, true);
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_OUTPUT");

            try (SocketChannel channel = server.accept())
            {
                k3po.notifyBarrier("ROUTED_INPUT");

                channel.setOption(StandardSocketOptions.SO_LINGER, 0);
                channel.close();

                k3po.finish();
            }
        }
    }

    @Test
    @Specification({
        "${route}/output/new/controller",
        "${streams}/server.then.client.sent.end/client/source"
    })
    public void endAfterIOExceptionFromReadShouldNotCauseReset() throws Exception
    {
        try (ServerSocketChannel server = ServerSocketChannel.open())
        {
            server.setOption(SO_REUSEADDR, true);
            server.bind(new InetSocketAddress("127.0.0.1", 0x1f90));

            k3po.start();
            k3po.awaitBarrier("ROUTED_OUTPUT");

            try (SocketChannel channel = server.accept())
            {
                k3po.notifyBarrier("ROUTED_INPUT");

                channel.setOption(StandardSocketOptions.SO_LINGER, 0);
                channel.close();

                k3po.finish();
            }
        }
    }

}
