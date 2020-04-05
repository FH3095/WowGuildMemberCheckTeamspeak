package eu._4fh.tsgroupguildsync.rest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.jackson.JacksonFeature;

import eu._4fh.tsgroupguildsync.Config;

public class RestHelper {
	private final @Nonnull Config config;

	public RestHelper(final @Nonnull Config config) {
		this.config = config;
	}

	public @Nonnull URI getAuthStartUrl(final long accountId) {
		final String systemName = config.getWebserviceSystemName();
		final String strAccountId = String.valueOf(accountId);
		final String redirectTo = config.getWebserviceAfterAuthRedirectTo();
		final UriBuilder result = UriBuilder.fromUri(config.getWebserviceUrl()).path("auth/start")
				.queryParam("systemName", systemName).queryParam("remoteId", strAccountId)
				.queryParam("redirectTo", redirectTo).queryParam("mac", calcMac(systemName, strAccountId, redirectTo));
		return result.build();
	}

	public @Nonnull List<Long> getAllAccountIds() {
		final String systemName = config.getWebserviceSystemName();
		final URI uri = UriBuilder.fromUri(config.getWebserviceUrl()).path("accounts/remoteIdsByRemoteSystem")
				.queryParam("systemName", systemName).queryParam("mac", calcMac(systemName)).build();
		final List<Long> result = createClient().target(uri).request(MediaType.APPLICATION_JSON)
				.get(new GenericType<List<Long>>() {
				});
		return result;
	}

	public boolean isOfficer(final long accountId) {
		final String systemName = config.getWebserviceSystemName();
		final String strAccountId = String.valueOf(accountId);
		final URI uri = UriBuilder.fromUri(config.getWebserviceUrl()).path("accounts/isOfficer")
				.queryParam("systemName", systemName).queryParam("remoteId", strAccountId)
				.queryParam("mac", calcMac(systemName, strAccountId)).build();
		final Boolean result = createClient().target(uri).request(MediaType.APPLICATION_JSON)
				.get(new GenericType<Boolean>() {
				});
		return Boolean.TRUE.equals(result);
	}

	private @Nonnull Client createClient() {
		try {
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
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private @Nonnull String calcMac(final String... values) {
		try {
			final Mac mac = Mac.getInstance(config.macAlgorithm());
			mac.init(config.getWebserviceMacKey());
			for (final String value : values) {
				mac.update(value.getBytes(StandardCharsets.UTF_8));
			}
			final byte[] macResult = mac.doFinal();
			return Base64.getEncoder().encodeToString(macResult);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
}
