package org.cache2k.test.core;

/*
 * #%L
 * cache2k implementation
 * %%
 * Copyright (C) 2000 - 2019 headissue GmbH, Munich
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheManager;
import org.cache2k.configuration.CacheTypeCapture;
import org.cache2k.core.InternalCache;
import org.cache2k.core.StandardExceptionPropagator;
import org.cache2k.core.util.Log;
import org.cache2k.event.CacheClosedListener;
import org.cache2k.integration.FunctionalCacheLoader;
import org.cache2k.testing.category.FastTests;
import static org.cache2k.test.core.StaticUtil.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Cache builder tests for some special variants.
 *
 * @see Cache2kBuilder
 */
@Category(FastTests.class)
public class Cache2kBuilderTest {

  private static final String CLASSNAME = Cache2kBuilderTest.class.getName();

  @Test
  public void managerName() {
    Cache c = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    assertEquals("default", c.getCacheManager().getName());
    c.close();
  }

  @Test
  public void autoGeneratedCacheName() {
    Cache c1 = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    assertTrue(c1.getName().startsWith("_org.cache2k"));
    Cache c2 = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    assertTrue(c1 != c2);
    c1.close();
    c2.close();
  }

  @Test
  public void typesParameters() {
    Cache<Long, String> c =
      Cache2kBuilder.forUnknownTypes()
        .valueType(String.class)
        .keyType(Long.class)
        .eternal(true)
        .build();
    assertEquals(Long.class, ((InternalCache<Long, String>) c).getKeyType().getType());
    assertEquals(String.class, ((InternalCache<Long, String>) c).getValueType().getType());
    c.close();
  }

  @Test
  public void noTypes() {
    Cache c =
      Cache2kBuilder.forUnknownTypes().eternal(true).build();
    c.put("hallo", 234);
    c.close();
  }

  /**
   * If types are unknown you can cast to every generic type.
   */
  @Test
  public void noTypesCastObjectObject() {
    Cache<Object, Object> c = (Cache<Object,Object>)
      Cache2kBuilder.forUnknownTypes().eternal(true).build();
    c.put("hallo", 234);
    c.close();
  }

  /**
   * If types are unknown you can cast to every generic type.
   */
  @Test
  public void noTypesCastStringInt() {
    Cache<String, Integer> c = (Cache<String,Integer>)
      Cache2kBuilder.forUnknownTypes().eternal(true).build();
    c.put("hallo", 234);
    c.close();
  }

  @Test
  public void noTypesAnyCache() {
    Cache<?, ?> c = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    c.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void anonymousWithoutTypes() {
    Cache c = new Cache2kBuilder(){}.eternal(true).build();
    c.clear();
  }

  @Test(expected = IllegalArgumentException.class)
  public void anonymousWithObjectObject() {
    Cache c = new Cache2kBuilder<Object,Object>(){}.eternal(true).build();
    c.clear();
  }

  @Test
  public void collectionValueType() {
    Cache<Long, List<String>> c =
      new Cache2kBuilder<Long, List<String>>() {}
        .eternal(true)
        .build();
    c.close();
  }

  /**
   * When using classes for the type information, there is no generic type information.
   * There is an ugly cast to (Object) needed to strip the result and be able to cast
   * to the correct one.
   */
  @Test
  public void collectionValueClass() {
    Cache<Long, List<String>> c =
      (Cache<Long, List<String>>) (Object) Cache2kBuilder.of(Long.class, List.class).eternal(true).build();
    c.put(123L, new ArrayList<String>());
    c.close();
  }

  /**
   * Use the cache type to specify a type with generic parameter. No additional cast is needed.
   */
  @Test
  public void collectionValueCacheType() {
    Cache<Long, List<String>> c =
      Cache2kBuilder.forUnknownTypes()
        .keyType(Long.class)
        .valueType(new CacheTypeCapture<List<String>>() {})
        .eternal(true)
        .build();
    c.put(123L, new ArrayList<String>());
    c.close();
  }

  @Test
  public void typesParametersWithList() {
    Cache<Long, List> c =
      Cache2kBuilder.forUnknownTypes()
        .valueType(List.class)
        .keyType(Long.class)
        .eternal(true)
        .build();
    c.close();
  }

  @Test
  public void noTypesAndCast() {
    Cache<Long, List<String>> c =
      (Cache<Long, List<String>>)
        Cache2kBuilder.forUnknownTypes()
          .eternal(true)
          .build();
    c.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void arrayKeyYieldsException() {
    new Cache2kBuilder<Integer[], String>() {}.build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void arrayValueYieldsException() {
    new Cache2kBuilder<String, Integer[]>() {}.build();
  }

  @Test
  public void cacheNameForAnnotationDefault() {
    Cache<Long, List<String>> c =
      (Cache<Long, List<String>>)
        Cache2kBuilder.forUnknownTypes()
          .eternal(true)
          .name("package.name.ClassName.methodName(package.ParameterType,package.ParameterType")
          .build();
    c.close();
  }

  @Test
  public void cacheNameInConstructor0() {
    Cache c = new BuildCacheInConstructor0().cache;
    assertThat(c.getName(), startsWith("_" + CLASSNAME + "$BuildCacheInConstructor0.INIT"));
    c.close();
  }

  @Test
  public void cacheNameInConstructor1() {
    Cache c = new BuildCacheInConstructor1().cache;
    assertThat(c.getName(), startsWith("_" + CLASSNAME + "$BuildCacheInConstructor1.INIT"));
    c.close();
  }

  @Test
  public void cacheNameInConstructor2() {
    Cache c = new BuildCacheInConstructor2().cache;
    assertThat(c.getName(), startsWith("_" + CLASSNAME + "$BuildCacheInConstructor2.INIT"));
    c.close();
  }

  @Test
  public void cacheNameInClassConstructor0() {
    Cache c = BuildCacheInClassConstructor0.cache;
    assertThat(c.getName(),
      startsWith("_" + CLASSNAME + "$BuildCacheInClassConstructor0.CLINIT"));
    c.close();
  }

  @Test
  public void duplicateCacheName() {
    String _managerName = getClass().getName() + ".duplicateCacheName";
    Log.registerSuppression(CacheManager.class.getName() + ":" + _managerName, new Log.SuppressionCounter());
    CacheManager mgr = CacheManager.getInstance(_managerName);
    try {
      Cache c0 = Cache2kBuilder.forUnknownTypes()
        .manager(mgr)
        .eternal(true)
        .name(this.getClass(), "same")
        .build();
      Cache c1 = Cache2kBuilder.forUnknownTypes()
        .manager(mgr)
        .eternal(true)
        .name(this.getClass(), "same")
        .build();
      fail("exception expected");
    } catch (IllegalStateException ex) {

    }
    mgr.close();
  }

  @Test(expected = IllegalStateException.class)
  public void managerAfterOtherStuff() {
    Cache2kBuilder.forUnknownTypes()
      .eternal(true)
      .manager(CacheManager.getInstance())
      .build();
  }

  @Test
  public void cacheCapacity10() {
    Cache c0 = Cache2kBuilder.forUnknownTypes()
      .entryCapacity(10)
      .build();
    assertEquals(10, latestInfo(c0).getHeapCapacity());
    c0.close();
  }

  final static String ILLEGAL_CHARACTERS_IN_NAME =
    "{}|\\^&=\";:<>*?/" +
      ((char) 27) +
      ((char) 127) +
      ((char) 128) +
      "äßà\ufefe";

  @Test
  public void illegalCharacterInCacheName_unsafeSet() {
    for (char c : ILLEGAL_CHARACTERS_IN_NAME.toCharArray()) {
      try {
        Cache _cache = Cache2kBuilder.forUnknownTypes()
          .name("illegalCharName" + c)
          .build();
        _cache.close();
        fail("Expect exception for illegal name in character '" + c + "', code " + ((int) c));
      } catch (IllegalArgumentException ex) {
      }
    }
  }

  @Test
  public void legalCharacterInCacheName() {
    String _legalChars = ".~,@ ()";
    _legalChars += "$-_abcABC0123";
    Cache c = Cache2kBuilder.forUnknownTypes()
      .name(_legalChars)
      .build();
    c.close();
  }

  @Test
  public void cacheCapacityDefault2000() {
    Cache c0 = Cache2kBuilder.forUnknownTypes().build();
    assertEquals(2000, latestInfo(c0).getHeapCapacity());
    c0.close();
  }

  /**
   * Check that long is getting through completely.
   */
  @Test
  public void cacheCapacityUnlimitedLongMaxValue() {
    Cache c0 = Cache2kBuilder.forUnknownTypes()
      .entryCapacity(Long.MAX_VALUE)
      .build();
    assertEquals(Long.MAX_VALUE, latestInfo(c0).getHeapCapacity());
    c0.close();
  }

  @Test
  public void cacheRemovedAfterClose() {
    final String _NAME = this.getClass().getSimpleName() + "-cacheRemovedAfterClose";
    CacheManager cm = CacheManager.getInstance(_NAME);
    Cache c = Cache2kBuilder.forUnknownTypes()
      .manager(cm)
      .name(_NAME)
      .build();
    assertEquals(c, cm.getActiveCaches().iterator().next());
    c.close();
    assertFalse(cm.getActiveCaches().iterator().hasNext());
  }

  @Test
  public void cacheRemovedAfterClose_WiredCache() {
    final String _NAME = this.getClass().getSimpleName() + "-cacheRemovedAfterCloseWiredCache";
    CacheManager cm = CacheManager.getInstance(_NAME);
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes()
      .manager(cm)
      .name(_NAME);
    StaticUtil.enforceWiredCache(_builder);
    Cache c = _builder.build();
    assertEquals(c, cm.getActiveCaches().iterator().next());
    c.close();
    assertFalse(cm.getActiveCaches().iterator().hasNext());
  }

  private void cacheClosedEventFired(boolean _wiredCache) {
    final AtomicBoolean _FIRED = new AtomicBoolean();
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder = _builder.addCacheClosedListener(new CacheClosedListener() {
      @Override
      public void onCacheClosed(final Cache cache) {
        _FIRED.set(true);
      }
    });
    if (_wiredCache) {
     StaticUtil.enforceWiredCache(_builder);
    }
    Cache c = _builder.build();
    c.close();
    assertTrue(_FIRED.get());
  }

  @Test
  public void cacheClosedEventFired() {
    cacheClosedEventFired(false);
  }

  @Test
  public void cacheClosedEventFired_WiredCache() {
    cacheClosedEventFired(true);
  }

  @Test
  public void cacheNameUnique() {
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder.name("hello", this.getClass(), "field");
    assertEquals("hello~org.cache2k.test.core.Cache2kBuilderTest.field", _builder.toConfiguration().getName());
  }

  @Test
  public void cacheNameUniqueNull() {
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder.name(null, this.getClass(), "field");
    assertEquals("org.cache2k.test.core.Cache2kBuilderTest.field", _builder.toConfiguration().getName());
  }

  @Test(expected = NullPointerException.class)
  public void cacheNameException() {
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder.name(this.getClass(), null);
  }

  @Test
  public void set_ExceptionPropagator() {
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder.exceptionPropagator(new StandardExceptionPropagator());
    assertNotNull(_builder.toConfiguration().getExceptionPropagator());
  }

  @Test
  public void set_storeByReference() {
    Cache2kBuilder _builder = Cache2kBuilder.forUnknownTypes();
    _builder.storeByReference(true);
    assertTrue(_builder.toConfiguration().isStoreByReference());
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildIntKey() {
    Cache2kBuilder _builder = Cache2kBuilder.of(String.class, String.class);
    _builder.buildForIntKey();
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildLongKey() {
    Cache2kBuilder _builder = Cache2kBuilder.of(String.class, String.class);
    _builder.buildForLongKey();
  }

  @Test(expected = IllegalArgumentException.class)
  public void refreshAheadButNoLoader() {
    Cache c = Cache2kBuilder.forUnknownTypes()
      .refreshAhead(true)
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .build();
    c.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void refreshAheadButNoExpiry() {
    Cache c = Cache2kBuilder.forUnknownTypes()
      .loader(new FunctionalCacheLoader() {
        @Override
        public Object load(final Object key) throws Exception {
          return null;
        }
      })
      .refreshAhead(true).build();
    c.close();
  }

  static class BuildCacheInConstructor0 {
    Cache<?,?> cache = Cache2kBuilder.forUnknownTypes().eternal(true).build();
  }

  static class BuildCacheInConstructor1 {

    Cache<?,?> cache;

    {
      cache = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    }
  }

  static class BuildCacheInConstructor2 {
    Cache<?,?> cache;
    BuildCacheInConstructor2() {
      cache = Cache2kBuilder.forUnknownTypes().eternal(true).build();
    }
  }

  static class BuildCacheInClassConstructor0 {
    static Cache<?,?> cache =
      Cache2kBuilder.forUnknownTypes().eternal(true).build();
  }

}
