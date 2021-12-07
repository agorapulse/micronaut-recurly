/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2021 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.recurly

// tag::code[]
import com.recurly.v3.Client
import com.recurly.v3.resources.Account
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Produces
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

class RecurlyFactorySpec extends Specification {

    private static final String API_KEY = 'someapikey'
    private static final String ACCOUNT_ID = 'account-id'

    @AutoCleanup
    @Shared
    EmbeddedServer recurlyServer = ApplicationContext.run(EmbeddedServer, ["spec.name": "RecurlyFactorySpec"]) as EmbeddedServer // <1>

    @Requires({env['RECURLY_INSECURE'] == 'true'})
    void 'client is configured'() {
        given:
        ApplicationContext ctx = ApplicationContext.run(['recurly.api-key': API_KEY,
                                                         'recurly.api-url': "http://localhost:$recurlyServer.port"]) // <2>

        expect:
        invocations() == 0
        ctx.containsBean(Client)

        when:
        Client recurlyClient = ctx.getBean(Client)
        Account account = recurlyClient.getAccount(ACCOUNT_ID) // <3>

        then:
        account.id == ACCOUNT_ID
        invocations() == old(invocations()) + 1 // <4>
    }

    private int invocations() {
        recurlyServer.applicationContext.getBean(RecurlyController).authorizedInvocations
    }

    @io.micronaut.context.annotation.Requires(property = 'spec.name', value = 'RecurlyFactorySpec')
    @Controller("/accounts")
    static class RecurlyController {
        int authorizedInvocations = 0
        private final String accountJson
        private final String token

        RecurlyController() {
            this.accountJson = RecurlyFactorySpec.getResourceAsStream('account.json').text
            this.token = "Basic " + Base64.encoder.encodeToString("$API_KEY:".bytes)
        }

        @Produces(RecurlyConfiguration.MEDIA_TYPE)
        @Get("/account-id")
        HttpResponse<?> detail(@Header String authorization) {
            if (authorization != token) {
                return HttpResponse.unauthorized()
            }
            authorizedInvocations++
            HttpResponse.ok(accountJson)
        }
    }

}
// end::code[]
