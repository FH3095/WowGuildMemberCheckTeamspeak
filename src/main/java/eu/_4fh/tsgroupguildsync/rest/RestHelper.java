package eu._4fh.tsgroupguildsync.rest;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jackson.JacksonFeature;

import eu._4fh.tsgroupguildsync.Config;

public class RestHelper {
	private final @Nonnull Config config;

	public RestHelper(final @Nonnull Config config) {
		this.config = config;
	}

	public @Nonnull UriBuilder createUri(final @Nonnull String endPoint, final @Nonnull String method) {
		UriBuilder result = UriBuilder.fromUri(config.getWebserviceUrl());
		result = result.path("rest/{guildId}/{endPoint}/{method}").queryParam("apiKey", config.getWebserviceApiKey())
				.queryParam("systemName", config.getWebserviceSystemName());
		result = result.resolveTemplate("guildId", config.getGuildId()).resolveTemplate("endPoint", endPoint)
				.resolveTemplate("method", method);
		return result;
	}

	public @Nonnull Client getIgnoreSslClient() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslcontext = SSLContext.getInstance("TLS");

		sslcontext.init(null, new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} }, new java.security.SecureRandom());

		return ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier((s1, s2) -> true)
				.register(JacksonFeature.class).build();
	}

	public @Nonnull String calcMac(final @Nonnull long accountId) {
		try {
			String toMacValues = String.valueOf(config.getGuildId()) + config.getWebserviceSystemName()
					+ String.valueOf(accountId);
			SecretKeySpec key = new SecretKeySpec(config.getWebserviceMacKey(), "HmacSHA256");
			Mac localMac = Mac.getInstance("HmacSHA256");
			localMac.init(key);
			byte[] localMacResult = localMac.doFinal(toMacValues.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(localMacResult);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
}
