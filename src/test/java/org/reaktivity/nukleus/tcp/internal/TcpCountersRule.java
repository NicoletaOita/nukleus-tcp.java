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
package org.reaktivity.nukleus.tcp.internal;

import static org.junit.Assert.assertEquals;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.reaktivity.reaktor.test.ReaktorRule;

public class TcpCountersRule implements TestRule
{
    private final ReaktorRule reaktor;

    public TcpCountersRule(ReaktorRule reaktor)
    {
        this.reaktor = reaktor;
    }

    @Override
    public Statement apply(Statement base, Description description)
    {
        return new Statement()
        {

            @Override
            public void evaluate() throws Throwable
            {
                TcpController controller = controller();
                assertEquals(0, controller.count("streams"));
                assertEquals(0, controller.count("routes"));
                assertEquals(0, controller.count("overflows"));
                base.evaluate();
            }

        };
    }

    public long routes()
    {
        return controller().count("routes");
    }

    public long streams()
    {
        return controller().count("streams");
    }

    public long overflows()
    {
        return controller().count("overflows");
    }

    private TcpController controller()
    {
        return reaktor.controller(TcpController.class);
    }

}
