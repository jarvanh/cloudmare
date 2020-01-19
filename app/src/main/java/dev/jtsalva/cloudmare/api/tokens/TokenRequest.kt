package dev.jtsalva.cloudmare.api.tokens

import dev.jtsalva.cloudmare.CloudMareActivity
import dev.jtsalva.cloudmare.api.Request
import dev.jtsalva.cloudmare.api.Response

class TokenRequest(context: CloudMareActivity) : Request<TokenRequest>(context) {

    suspend fun verify(): Response {
            requestTAG = GET
            return super.httpGet("/user/tokens/verify")
        }

}