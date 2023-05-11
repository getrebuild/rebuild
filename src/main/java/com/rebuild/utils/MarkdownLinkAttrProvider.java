/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.html.MutableAttributes;

/**
 * @author devezhao
 * @since 2023/5/11
 */
public class MarkdownLinkAttrProvider {

    static class MarkdownLinkAttrExtension implements HtmlRenderer.HtmlRendererExtension {
        @Override
        public void rendererOptions(MutableDataHolder options) {
        }
        @Override
        public void extend(HtmlRenderer.Builder htmlRendererBuilder, String rendererType) {
            htmlRendererBuilder.attributeProviderFactory(AttributeProviderImpl.Factory());
        }

        static MarkdownLinkAttrExtension create() {
            return new MarkdownLinkAttrExtension();
        }
    }

    static class AttributeProviderImpl implements AttributeProvider {
        @Override
        public void setAttributes(Node node, AttributablePart part, MutableAttributes attributes) {
            if (node instanceof Link && part == AttributablePart.LINK) {
                Link link = (Link) node;
                String url = link.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    attributes.replaceValue("target", "_blank");
                }
            }
        }

        static AttributeProviderFactory Factory() {
            return new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider apply(LinkResolverContext context) {
                    return new AttributeProviderImpl();
                }
            };
        }
    }
}
