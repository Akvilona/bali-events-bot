package com.bali.events.balievents.service;

import com.bali.events.balievents.model.EventDto;
import com.bali.events.balievents.model.Scrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.bali.events.balievents.support.SeleniumUtils.getAttributeByClass;
import static com.bali.events.balievents.support.SeleniumUtils.getAttributeByXpath;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheBeatBaliScrapperService implements ScrapperService {

    public static final int MAX_MONTH_COUNT_WITH_EVENTS = 9;
    private static final By BY_EVENT_GROUP = By.xpath("/html/body/div[1]/div[2]/div/div/main/article/div/div/section[3]/div/div[2]/div/div[2]/div/div/div[4]/div");
    //                                                                /html/body/div[1]/div[2]/div/div/main/article/div/div/section[3]/div/div[2]/div/div[2]/div/div/div[4]/div
    //                                                                    /html/body/div[1]/div[2]/div/div/main/article/div/div/section[3]/div/div[2]/div/div[2]/div/div/div[4]/div
    //                                                                    /html/body/div[1]/div[2]/div/div/main/article/div/div/section[3]/div/div[2]/div/div[2]/div/div/div[4]/div[1]
    private static final By BY_BUTTON_WRAPPER = By.xpath("/html/body/div[1]/div[2]/div/div/main/article/div/div/section[3]/div/div[2]/div/div[2]/div/div/div[1]");

    private static final String BY_TOPICS = "./child::*";
    private final UpdateEventService updateEventService;
    private final WebDriver webDriver;

    private static boolean isChildIdNotExists(final WebElement child) {
        if (child == null) {
            log.warn("Child element is null");
            return true;
        }

        final String externalId = child.getAttribute("id");
        if (!StringUtils.hasText(externalId)) {
            log.warn("Empty externalId, skipping this element");
            return true;
        }

        return false;
    }

    /**
     * Здесь указывается сайт который мы будем считывать
     * https://thebeatbali.com/bali-events/
     *
     * @return - корневой домен сайта
     */
    @Override
    public String rootName() {
        return Scrapper.THE_BEAT_BALI.getRootName();
    }

    /**
     * Метод для скачивания первого сайта (для другого сайта нужно писать другую логику аналогичного метода)
     */
    @Override
    public void process() {
        navigateToWebsite(webDriver);

        for (int i = 0; i < MAX_MONTH_COUNT_WITH_EVENTS; i++) {
            final List<WebElement> parsedEventList = getParsedEventList(webDriver);
            log.info("Month proceeding [{} / {}]... Month events size: {} ", i, MAX_MONTH_COUNT_WITH_EVENTS, parsedEventList.size() - 1);

            // Если объектов менее 2, то это только объект рекламы у которого нет ID
            if (parsedEventList.size() <= 2) {
                continue;
            }

            processEvents(parsedEventList);
            navigateToNextMonth(webDriver);
        }

        webDriver.quit();
    }

    private void processEvents(final List<WebElement> parsedEventList) {
        for (int i = 0; i < parsedEventList.size(); i++) {
            final WebElement child = parsedEventList.get(i);

            if (isChildIdNotExists(child)) {
                continue;
            }

            final EventDto eventDto = createEventDto(child);
            updateEventService.saveOrUpdate(eventDto);
            log.info("Event processed [{} / {}]", i, parsedEventList.size() - 1);
        }
    }

    /**
     * Создаем и заполняем структуру полей (назовем это 'DTO')
     *
     * @param child - элемент веб-страницы
     * @return - заполненный DTO
     */
    private EventDto createEventDto(final WebElement child) {
        final String externalId = child.getAttribute("id");
        final String eventName = getAttributeByClass(child, "evcal_event_title", "innerHTML");
        final String locationName = getAttributeByClass(child, "event_location_attrs", "data-location_name");
        final String locationAddress = getAttributeByClass(child, "event_location_attrs", "data-location_address");
        final String startDate = getAttributeByXpath(child, "div/meta[2]", "content");
        final String endDate = getAttributeByXpath(child, "div/meta[3]", "content");
        final String eventUrl = getAttributeByXpath(child, "div/a", "href");
        final String imageUrl = getAttributeByClass(child, "ev_ftImg", "data-img");
        final String coordinates = getAttributeByClass(child, "evcal_location", "data-latlng");

        return EventDto.builder()
            .externalId(externalId)
            .eventName(eventName)
            .locationName(locationName)
            .locationAddress(locationAddress)
            .startDate(startDate)
            .endDate(endDate)
            .eventUrl(eventUrl)
            .imageUrl(imageUrl)
            // в том числе и поле где будет хранится корневое имя сайта
            // (Пример: 'https://thebeatbali.com/bali-events/') в таблица event поле service_name
            .serviceName(rootName())
            .coordinates(coordinates)
            .build();
    }

    /**
     * Получаем список событий из веб-страницы
     *
     * @param webDriver - открытый браузер
     * @return - список событий
     */
    private List<WebElement> getParsedEventList(final WebDriver webDriver) {
        return webDriver.findElement(BY_EVENT_GROUP).findElements(By.xpath(BY_TOPICS));
    }

    /**
     * Переходим на следующую страницу с событиями
     *
     * @param webDriver - открытый браузер
     */
    private void navigateToNextMonth(final WebDriver webDriver) {
        log.info("Switching to the next month...");
        webDriver.findElement(BY_BUTTON_WRAPPER).findElement(By.id("evcal_next")).click();
        delay(20000);
    }

    /**
     * Устанавливаем корневой домен скачиваемого сайта, и сайт появляется в открытом браузере
     * (метод может зависнуть а может проскочить и тогда следующим шагом делаем задержку )
     *
     * @param webDriver - открытый браузер
     */
    private void navigateToWebsite(final WebDriver webDriver) {
        webDriver.get(rootName());
        log.info("Web driver navigated to: " + rootName());
        delay(25000);
    }

    /**
     * Задержка для того чтобы сайт успел загрузиться
     *
     * @param milliseconds - время задержки в миллисекундах
     */
    private void delay(final int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
