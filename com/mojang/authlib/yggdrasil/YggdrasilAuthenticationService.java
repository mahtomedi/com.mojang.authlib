package com.mojang.authlib.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.Response;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {
   private final String clientToken;
   private final Gson gson = new Gson();

   public YggdrasilAuthenticationService(Proxy proxy, String clientToken) {
      super(proxy);
      this.clientToken = clientToken;
   }

   @Override
   public UserAuthentication createUserAuthentication(Agent agent) {
      return new YggdrasilUserAuthentication(this, agent);
   }

   @Override
   public MinecraftSessionService createMinecraftSessionService() {
      return new YggdrasilMinecraftSessionService(this);
   }

   protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
      try {
         String jsonResult = input == null ? this.performGetRequest(url) : this.performPostRequest(url, this.gson.toJson(input), "application/json");
         T result = (T)this.gson.fromJson(jsonResult, classOfT);
         if (result == null) {
            return null;
         } else if (StringUtils.isNotBlank(result.getError())) {
            if ("UserMigratedException".equals(result.getCause())) {
               throw new UserMigratedException(result.getErrorMessage());
            } else if (result.getError().equals("ForbiddenOperationException")) {
               throw new InvalidCredentialsException(result.getErrorMessage());
            } else {
               throw new AuthenticationException(result.getErrorMessage());
            }
         } else {
            return result;
         }
      } catch (IOException var6) {
         throw new AuthenticationUnavailableException("Cannot contact authentication server", var6);
      } catch (IllegalStateException var7) {
         throw new AuthenticationUnavailableException("Cannot contact authentication server", var7);
      } catch (JsonParseException var8) {
         throw new AuthenticationUnavailableException("Cannot contact authentication server", var8);
      }
   }

   public String getClientToken() {
      return this.clientToken;
   }
}
