package com.mojang.authlib.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileProperty;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
   private static final URL JOIN_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/join");
   private static final URL CHECK_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/hasJoined");
   private final PublicKey publicKey;
   private final Gson gson = new Gson();

   protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
      super(authenticationService);

      try {
         X509EncodedKeySpec spec = new X509EncodedKeySpec(
            IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der"))
         );
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");
         this.publicKey = keyFactory.generatePublic(spec);
      } catch (Exception var4) {
         throw new Error("Missing/invalid yggdrasil public key!");
      }
   }

   @Override
   public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
      JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
      request.accessToken = authenticationToken;
      request.selectedProfile = profile.getId();
      request.serverId = serverId;
      this.getAuthenticationService().makeRequest(JOIN_URL, request, Response.class);
   }

   @Override
   public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
      Map<String, Object> arguments = new HashMap();
      arguments.put("username", user.getName());
      arguments.put("serverId", serverId);
      URL url = HttpAuthenticationService.concatenateURL(CHECK_URL, HttpAuthenticationService.buildQuery(arguments));

      try {
         HasJoinedMinecraftServerResponse response = this.getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);
         if (response != null && response.getId() != null) {
            GameProfile result = new GameProfile(response.getId(), user.getName());
            if (response.getProperties() != null) {
               for(ProfileProperty property : response.getProperties()) {
                  result.getProperties().put(property.getName(), property);
               }
            }

            return result;
         } else {
            return null;
         }
      } catch (AuthenticationUnavailableException var9) {
         throw var9;
      } catch (AuthenticationException var10) {
         return null;
      }
   }

   @Override
   public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile) {
      ProfileProperty textureProperty = (ProfileProperty)profile.getProperties().get("textures");
      if (textureProperty == null) {
         return new HashMap();
      } else if (!textureProperty.hasSignature()) {
         LOGGER.error("Signature is missing from textures payload");
         return new HashMap();
      } else if (!textureProperty.isSignatureValid(this.publicKey)) {
         LOGGER.error("Textures payload has been tampered with (signature invalid)");
         return new HashMap();
      } else {
         MinecraftTexturesPayload result;
         try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            result = (MinecraftTexturesPayload)this.gson.fromJson(json, MinecraftTexturesPayload.class);
         } catch (JsonParseException var5) {
            LOGGER.error("Could not decode textures payload", var5);
            return new HashMap();
         }

         if (result.getProfileId() == null || !result.getProfileId().equals(profile.getId())) {
            LOGGER.error(
               "Decrypted textures payload was for another user (expected id {} but was for {})", new Object[]{profile.getId(), result.getProfileId()}
            );
            return new HashMap();
         } else if (result.getProfileName() != null && result.getProfileName().equals(profile.getName())) {
            return (Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>)(result.getTextures() == null ? new HashMap() : result.getTextures());
         } else {
            LOGGER.error(
               "Decrypted textures payload was for another user (expected name {} but was for {})", new Object[]{profile.getName(), result.getProfileName()}
            );
            return new HashMap();
         }
      }
   }

   public YggdrasilAuthenticationService getAuthenticationService() {
      return (YggdrasilAuthenticationService)super.getAuthenticationService();
   }
}
