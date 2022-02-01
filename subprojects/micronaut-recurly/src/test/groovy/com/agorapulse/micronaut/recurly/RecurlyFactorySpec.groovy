/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 Agorapulse.
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
import com.stehno.ersatz.ContentType
import com.stehno.ersatz.ErsatzServer
import io.micronaut.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

import static com.github.stefanbirkner.systemlambda.SystemLambda.*

class RecurlyFactorySpec extends Specification {

    private static final String API_KEY = 'someapikey'
    private static final String ACCOUNT_ID = 'account-id'

    @AutoCleanup ApplicationContext context
    @AutoCleanup ErsatzServer server

    Client recurlyClient

    void 'client is configured'() {
        given:
            String token = Base64.encoder.encodeToString("$API_KEY:".bytes)
            String accountJson = RecurlyFactorySpec.getResourceAsStream('account.json').text

            server = new ErsatzServer({                                                 // <1>
                reportToConsole()
            })

            server.expectations {
                get '/accounts/' + ACCOUNT_ID, {
                    header 'Authorization', "Basic $token"
                    responds().body(accountJson, ContentType.APPLICATION_JSON)
                }
            }

            server.start()

        when:
            Account account = withEnvironmentVariable('RECURLY_INSECURE', 'true').execute { // <2>
                context = ApplicationContext.builder(
                    'recurly.api-key': API_KEY,
                    'recurly.api-url': server.httpUrl                                   // <3>
                ).build()

                context.start()

                recurlyClient = context.getBean(Client)

                recurlyClient.getAccount(ACCOUNT_ID)                                    // <4>
            }

        then:
            server.verify()

            account.id == ACCOUNT_ID                                                    // <5>
    }

}
// end::code[]
