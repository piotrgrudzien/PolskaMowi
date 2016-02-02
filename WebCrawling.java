package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawling {
	static ArrayList<String> linkList = new ArrayList<String>();
	static int stepsIntoLinks = 22;
	static String root[] = { "OnetWiadomosci", "GazetaWiadomosci", "WPWiadomosci", "InteriaWiadomosci", "O2SforaWiadomosci", "TVN24Wiadomosci",
			"Wyborcza", "Fakt", "Newsweek" };
	static String mainPage[] = { "onet", "gazeta", "wp", "interia", "sfora", "tvn24", "wyborcza", "fakt", "newsweek" };
	static String wiadomosciPage[] = { "wiadomosci.onet.pl", "wiadomosci.gazeta.pl", "wiadomosci.wp.pl", "fakty.interia.pl", "www.sfora.pl",
			"www.tvn24.pl", "wyborcza.pl", "www.fakt.pl/wydarzenia", "NewsweekIsSpecial" };

	private void crawlStep(String link, String wiadomosciPage, int counter, PrintWriter out, int index) throws IOException {
		if (counter > stepsIntoLinks) {
			return;
		}
		System.out.println("Crawl step number " + counter);
		System.out.println("Address: " + link);
		linkList.add(link);
		counter++;
		String html = Jsoup.connect(link).timeout(10000).get().html();
		Document doc = Jsoup.parse(html);
		Elements links = doc.getElementsByTag("a");
		Elements span = null;
		Elements p = null;
		Elements meta = null;
		Elements div = null;
		Elements article = null;
		boolean wroteSomething = false;
		if (isWiadomosci(link, index)) {
			span = doc.select("span");
			p = doc.getElementsByTag("p");
			meta = doc.select("meta");
			if (index == 1) {
				div = doc.getElementsByTag("div");
			}
			if (index == 2) {
				article = doc.getElementsByTag("article");
			}/*
			 * System.out.println("Span = " + span); System.out.println("P = " +
			 * p); System.out.println("Meta = " + meta);
			 * System.out.println("Div = " + div);
			 * System.out.println("Article = " + article);
			 */
			wroteSomething = getAllText(span, p, meta, div, article, out, index, false);
		}
		if (wroteSomething) {
			out.print(" ");
			out.println(); // New line only after a full website is done
		}

		for (Element alink : links) {
			/*
			 * System.out.println("Link: " + alink.attr("abs:href"));
			 * System.out.println("Visited: " +
			 * visited(alink.attr("abs:href"))); System.out.println("isWithin: "
			 * + isWithin(alink.attr("abs:href"), mainPage[index]));
			 * System.out.println("isSpecial: " +
			 * isSpecial(alink.attr("abs:href"), counter, index));
			 */if (!visited(alink.attr("abs:href"), index) && isWithin(alink.attr("abs:href"), mainPage[index])
					&& isSpecial(alink.attr("abs:href"), counter, index)) {
				try {
					crawlStep(alink.attr("abs:href"), wiadomosciPage, counter, out, index);
				} catch (Exception pe) {
				}
			}
		}
		if (index == 5) {
			Elements linksGuid = doc.getElementsByTag("guid");
			for (Element alink : linksGuid) {
				if (!visited(alink.text(), index) && isWithin(alink.text(), mainPage[index]) && isSpecial(alink.text(), counter, index)) {
					try {
						crawlStep(alink.text(), wiadomosciPage, counter, out, index);
					} catch (Exception pe) {
					}
				}
			}
		}
	}

	private boolean visited(String newLink, int index) {
		for (String link : linkList) {
			if (newLink.length() >= link.length()) {
				if (link.equals(newLink)) {
					return true;
				}
				if (index == 1) {
					if (link.equals(newLink.substring(0, link.length()))) {
						/*
						 * If doesn't contain a / then it's some link to
						 * comments rather than a subpage
						 */
						if (!newLink.substring(link.length(), newLink.length()).contains("/")) {
							/*
							 * System.out.println("NEW LINK: " + newLink);
							 * System.out.println("CHECK LINK: " + link);
							 * System.out.println("SUBSTRING: " +
							 * newLink.substring(link.length(),
							 * newLink.length()));
							 */
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isSpecial(String link, int step, int index) {
		boolean result = isWiadomosci(link, index);
		return result || step < 6;
	}

	private boolean isWiadomosci(String link, int index) {
		String pattern = "http://" + wiadomosciPage[index];
		// Special case for newsweek
		if (index == 8) {
			pattern = "http://" + "swiat.newsweek.pl";
			String pattern2 = "http://" + "polska.newsweek.pl";
			if (link.length() < pattern2.length()) {
				return false;
			}
			return link.startsWith(pattern) || link.startsWith(pattern2);
		} else {
			if (link.length() < pattern.length()) {
				return false;
			}
			return link.startsWith(pattern);
		}
	}

	private boolean getAllText(Elements span, Elements p, Elements meta, Elements div, Elements article, PrintWriter out, int index, boolean debug) {
		boolean wroteSomething = false;
		ArrayList<String> tempList = new ArrayList<String>();
		String text = "";
		Elements[] allElements = { span, p, meta, div, article };
		for (Elements elements : allElements) {
			if (elements == null) {
				continue;
			}
			if (debug) {
				if (elements == span) {
					System.out.println("Looking at " + span.size() + " SPAN elements: ");
				}
				if (elements == p) {
					System.out.println("Looking at " + p.size() + " P elements: ");
				}
				if (elements == meta) {
					System.out.println("Looking at " + meta.size() + " META elements: ");
				}
				if (elements == div) {
					System.out.println("Looking at " + div.size() + " DIV elements: ");
				}
				if (elements == article) {
					System.out.println("Looking at " + article.size() + " ARTICLE elements: ");
				}
			}
			for (Element element : elements) {
				if (element == null) {
					continue;
				}
				if (elements == meta) {
					text = element.attr("content");
				} else {
					text = element.text();
				}
				/*
				 * Gazeta or wyborcza id=artykul div I ignore a div if it's
				 * neither gazeta nor wyborcza or it's id is not artykul
				 */
				if (elements == div) {
					if ((index != 1 && index != 6) || !element.attr("id").equals("artykul")) {
						continue;
					}
				}
				/*
				 * WP article tag - taken care of by the null check
				 */
				if (text.length() < 150 || text.contains("http://")) {
					continue;
				}
				text = text.replaceAll("[\\t\\n\\r]", " ");
				if (!tempList.contains(text)) {
					if (debug) {
						System.out.println("About to write: ");
						System.out.println(text);
					}
					out.print(text);
					wroteSomething = true;
					tempList.add(text);
				}
			}
		}
		return wroteSomething;
	}

	// Here checking if we remain on the correct website
	private boolean isWithin(String link, String mainPage) {
		return link.contains(mainPage + ".pl");
	}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		WebCrawling webCrawling = new WebCrawling();
		for (int i = 0; i < root.length; i++) {
			PrintWriter out = new PrintWriter(root[i] + ".txt");
			if (i == 6) {
				// very dodgy, test it often!!!
				webCrawling.crawlStep("http://" + mainPage[i] + ".pl/0,0.html?piano_t=1", wiadomosciPage[i], 0, out, i);
			} else {
				webCrawling.crawlStep("http://" + mainPage[i] + ".pl/", wiadomosciPage[i], 0, out, i);
			}
			out.close();
			PrintWriter linksFile = new PrintWriter(root[i] + "links.txt");
			for (String link : linkList) {
				linksFile.println(link);
			}
			linksFile.close();
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			System.out.println("Finished: " + root[i]);
			System.out.println("Links visited: " + linkList.size());
			linkList.clear();
			System.out.println("It took " + duration / 1000 + " s");
			System.out.println("Or " + duration / 60000 + " min");
		}
		System.out.println("FINISHED THE JOB SUCCESSFULLY!");
	}
}
