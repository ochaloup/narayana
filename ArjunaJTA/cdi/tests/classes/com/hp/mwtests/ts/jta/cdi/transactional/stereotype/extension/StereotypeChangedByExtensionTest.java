/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.hp.mwtests.ts.jta.cdi.transactional.stereotype.extension;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.hp.mwtests.ts.jta.cdi.transactional.stereotype.TransactionalRequiredStereotype;

@RunWith(Arquillian.class)
public class StereotypeChangedByExtensionTest {
    @Inject
    NoAnnotationBean bean;

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "stereotype-test.war")
        	.addPackage(StereotypeChangedByExtensionTest.class.getPackage())
        	.addClasses(TransactionalRequiredStereotype.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, AddStereotypeAnnotationExtension.class);
    }


    @Test(expected = RuntimeException.class)
    public void stereotypeAddedByExtensionAtBean() throws Exception {
        bean.process();
    }
}