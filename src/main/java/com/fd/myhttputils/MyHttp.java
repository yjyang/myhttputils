package com.fd.myhttputils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 通过 Java 发送 HTTP 请求
 * 
 * @author 符冬
 *
 */
public final class MyHttp {
	private static final String JSON_CONTENT_TYPE = "application/json";
	public final static String MULTIPART_CONTENT_TYPE = "multipart/form-data";
	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private static final int D_TIMEOUT = 10000;

	private static URLConnection openConnection(String url, Proxy proxy) {
		try {
			if (proxy != null) {
				return new URL(url).openConnection(proxy);
			} else {
				return new URL(url).openConnection();
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 获取响应状态码
	 * 
	 * @param url
	 * @param contentType
	 * @param cookie
	 * @param proxy
	 * @return
	 */
	public static int getResponseCode(String url, String contentType, String cookie, Proxy proxy) {
		return getResponseCode(url, contentType, cookie, proxy, D_TIMEOUT);
	}

	/**
	 * 获取响应状态码
	 * 
	 * @param url
	 * @param contentType
	 * @param cookie
	 * @param proxy
	 * @param timeout
	 * @return
	 */
	public static int getResponseCode(String url, String contentType, String cookie, Proxy proxy, int timeout) {
		URLConnection conn = getUrlConnection(url, contentType, cookie, proxy, timeout);
		return getResponseCode(conn);
	}

	/**
	 * 获取重定向地址
	 * 
	 * @param conn
	 * @return
	 */
	public static String getLocation(URLConnection conn) {
		return conn.getHeaderField("Location");
	}

	/**
	 * 获取响应状态码
	 * 
	 * @param conn
	 * @return
	 */
	public static int getResponseCode(URLConnection conn) {
		String headerField = conn.getHeaderField(0);
		return Integer.valueOf(headerField.split("\\s+")[1]);
	}

	public static URLConnection getURLConnection(String url, String contentType, String cookie, int timeout) {
		return getUrlConnection(url, contentType, cookie, null, timeout);
	}

	public static URLConnection getURLConnection(String url, String contentType, String cookie) {
		return getUrlConnection(url, contentType, cookie, null, D_TIMEOUT);
	}

	public static URLConnection getUrlConnection(String url, String contentType, String cookie, Proxy proxy) {
		return getUrlConnection(url, contentType, cookie, proxy, D_TIMEOUT);
	}

	public static URLConnection getUrlConnection(String url, String contentType, String cookie, Proxy proxy,
			int timeout) {
		try {
			URLConnection conn = openConnection(url, proxy);
			conn.setConnectTimeout(timeout);
			if (!conn.getDoOutput()) {
				conn.setDoOutput(true);
			}
			if (cookie != null && cookie.trim().length() > 1) {
				conn.addRequestProperty("Cookie", cookie);
			}
			if (contentType != null && contentType.trim().length() > 1) {
				conn.setRequestProperty("Content-Type", contentType);
			}
			conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
			conn.setRequestProperty("User-Agent", String.format("%s/%s/%s/%s/%s/%s/%s/%s", "Windows", System.nanoTime(),
					"Chrome", "Safari", "QQBrowser", "Mozilla", "Firefox", "IE"));
			return conn;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 文件下载
	 * 
	 * @return
	 */
	public static byte[] getData(String url) {
		URLConnection conn = getURLConnection(url, null, null);

		try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			byte[] b = new byte[4096];
			int len = bis.read(b);
			while (len != -1) {
				bao.write(b, 0, len);
				len = bis.read(b);
			}
			return bao.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 获取文本
	 * 
	 * @return
	 */
	public static String getContent(String url) {
		URLConnection conn = getURLConnection(url, null, null);
		return getResponseContent(conn, StandardCharsets.UTF_8);
	}

	public static String getResponseContent(URLConnection conn, Charset cs) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), cs))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			return sb.toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 表单提交
	 * 
	 * @param url
	 * @param inputs
	 * @return
	 */
	public static String postForm(String url, Map<String, String> inputs) {
		URLConnection conn = getURLConnection(url, FORM_CONTENT_TYPE, null);
		return postForm(inputs, conn);

	}

	/**
	 * 表单提交
	 * 
	 * @param inputs
	 * @param conn
	 * @return
	 */
	public static String postForm(Map<String, String> inputs, URLConnection conn) {
		try (OutputStream out = conn.getOutputStream()) {
			if (inputs != null && inputs.size() > 0) {
				Iterator<Entry<String, String>> ite = inputs.entrySet().iterator();
				StringBuilder sb = new StringBuilder();
				while (ite.hasNext()) {
					Entry<String, String> en = ite.next();
					sb.append(en.getKey()).append("=")
							.append(URLEncoder.encode(en.getValue(), StandardCharsets.UTF_8.toString()));
					if (ite.hasNext()) {
						sb.append("&");
					}
				}
				out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			}
			return getResponseContent(conn, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * JSON请求
	 * 
	 * @param url
	 * @param json
	 * @return
	 */
	public static String postJson(String url, String json) {
		URLConnection conn = getURLConnection(url, JSON_CONTENT_TYPE, null);
		try (OutputStream out = conn.getOutputStream()) {
			if (json != null && json.trim().length() > 0) {
				out.write(json.getBytes(StandardCharsets.UTF_8));
			}
			return getResponseContent(conn, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 表单请求数据
	 * 
	 * @param kvs
	 * @return
	 */
	public static Map<String, String> getMap(String... kvs) {
		if (kvs != null && kvs.length > 0 && kvs.length % 2 == 0) {
			Map<String, String> rq = new HashMap<String, String>(kvs.length % 2);
			for (int i = 0; i < kvs.length; i++) {
				rq.put(kvs[i], kvs[++i]);
			}
			return rq;
		}
		return null;
	}

	/**
	 * 图片上传
	 * 
	 * @param img
	 * @param url
	 * @return
	 */
	public static String uploadImage(byte[] img, String url) {
		return uploadFile(img, url, "img1", String.format("%s.%s", System.nanoTime(), "jpg"), "image/jpeg");
	}

	/**
	 * 文件上传
	 * 
	 * @param img
	 * @param url
	 * @param inputName
	 * @param filename
	 * @param contentType
	 * @return
	 */
	public static String uploadFile(byte[] img, String url, String inputName, String filename, String contentType) {
		URLConnection uc = getURLConnection(url, null, null);
		String boundary = String.format("----WebKitFormBoundary%s", System.nanoTime());
		uc.setRequestProperty("Content-Type", String.format("%s; boundary=%s", MULTIPART_CONTENT_TYPE, boundary));
		uc.setUseCaches(false);
		uc.setRequestProperty("Upgrade-Insecure-Requests", "1");
		try (OutputStream os = uc.getOutputStream();) {
			os.write(String.format("--%s", boundary).getBytes(StandardCharsets.UTF_8));
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			os.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", inputName, filename)
					.getBytes(StandardCharsets.UTF_8));
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			os.write(String.format("Content-Type: %s", contentType).getBytes(StandardCharsets.UTF_8));
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			os.write(img);
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			os.write(String.format("--%s--", boundary).getBytes(StandardCharsets.UTF_8));
			os.write(CRLF.getBytes(StandardCharsets.UTF_8));
			return getResponseContent(uc, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final String CRLF = "\r\n";
}
