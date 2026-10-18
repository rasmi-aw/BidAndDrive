package com.auction.app.web;

import com.auction.domain.shared.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Builds a HAL {@link PagedModel} from the domain {@link Page}, with self / prev / next links.
 */
public final class PagedModels {

    private PagedModels() {
    }

    public static <T> PagedModel<EntityModel<T>> of(Page<?> page, List<EntityModel<T>> content,
                                                    IntFunction<Link> linkToPage) {
        List<Link> links = new ArrayList<>();
        links.add(linkToPage.apply(page.page()).withSelfRel());
        if (page.page() > 0) {
            links.add(linkToPage.apply(page.page() - 1).withRel(IanaLinkRelations.PREV));
        }
        if (page.page() + 1 < page.totalPages()) {
            links.add(linkToPage.apply(page.page() + 1).withRel(IanaLinkRelations.NEXT));
        }
        PagedModel.PageMetadata metadata =
                new PagedModel.PageMetadata(page.size(), page.page(), page.totalElements(), page.totalPages());
        return PagedModel.of(content, metadata, links);
    }
}
