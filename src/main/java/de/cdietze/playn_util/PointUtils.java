package de.cdietze.playn_util;

import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import pythagoras.i.IDimension;
import pythagoras.i.IPoint;
import pythagoras.i.Point;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class PointUtils {

  public static final Ordering<IPoint> defaultOrdering = new Ordering<IPoint>() {
    @Override
    public int compare(IPoint left, IPoint right) {
      return ComparisonChain.start().compare(left.y(), right.y()).compare(left.x(), right.x()).result();
    }
  };
  public static int toX(IDimension dim, int index) { return index % dim.width(); }
  public static int toY(IDimension dim, int index) { return index / dim.width(); }
  public static int toIndex(IDimension dim, int x, int y) { return x + y * dim.width(); }
  public static int toIndex(IDimension dim, IPoint p) { return toIndex(dim, p.x(), p.y()); }
  public static Point toPoint(IDimension dim, int index) {
    return new Point(index % dim.width(), index / dim.width());
  }
  public static boolean contains(IDimension dim, int index) { return contains(dim, toX(dim, index), toY(dim, index)); }
  public static boolean contains(IDimension dim, int x, int y) { return x >= 0 && x < dim.width() && y >= 0 && y < dim.height(); }
  public static boolean contains(IDimension dim, IPoint p) { return p.x() >= 0 && p.x() < dim.width() && p.y() >= 0 && p.y() < dim.height(); }
  public static List<Integer> proxyPath(IDimension dim, int index) {
    return proxyPath(dim, index, Math.max(dim.width(), dim.height()));
  }

  /**
   * Returns a list of indexes with increasing distance to index.
   */
  public static List<Integer> proxyPath(IDimension dim, int index, int maxDist) {
    int x = toX(dim, index);
    int y = toY(dim, index);
    List<Integer> result = new ArrayList<>();
    result.add(index);
    for (int a = 0; a <= maxDist; ++a) {
      if (a != 0) {
        addIfContains(dim, result, x + a, y);
        addIfContains(dim, result, x, y + a);
        addIfContains(dim, result, x - a, y);
        addIfContains(dim, result, x, y - a);
      }
      for (int b = 1; b < a; ++b) {
        addIfContains(dim, result, x + a, y + b);
        addIfContains(dim, result, x + b, y + a);
        addIfContains(dim, result, x - b, y + a);
        addIfContains(dim, result, x - a, y + b);
        addIfContains(dim, result, x - a, y - b);
        addIfContains(dim, result, x - b, y - a);
        addIfContains(dim, result, x + b, y - a);
        addIfContains(dim, result, x + a, y - b);
      }
      if (a != 0) {
        addIfContains(dim, result, x + a, y + a);
        addIfContains(dim, result, x - a, y + a);
        addIfContains(dim, result, x - a, y - a);
        addIfContains(dim, result, x + a, y - a);
      }
    }
    return result;
  }

  private static void addIfContains(IDimension dim, Collection<Integer> coll, int x, int y) {
    if (contains(dim, x, y)) coll.add(toIndex(dim, x, y));
  }
  private static void setIfContains(IDimension dim, BitSet set, int x, int y) {
    if (contains(dim, x, y)) set.set(toIndex(dim, x, y));
  }

  public static List<Point> neighbors(IDimension dim, IPoint p) {
    ImmutableList.Builder<Point> builder = ImmutableList.builder();
    if (p.x() > 0) builder.add(new Point(p.x() - 1, p.y()));
    if (p.y() > 0) builder.add(new Point(p.x(), p.y() - 1));
    if (p.x() < dim.width() - 1) builder.add(new Point(p.x() + 1, p.y()));
    if (p.y() < dim.height() - 1) builder.add(new Point(p.x(), p.y() + 1));
    return builder.build();
  }

  /**
   * @return a BitSet with all orthogonal neighbors of index. Checks that the fields are in bounds of dim.
   */
  public static BitSet neighbors(IDimension dim, int index, BitSet result) {
    int x = toX(dim, index);
    int y = toY(dim, index);
    if (x > 0) result.set(toIndex(dim, x - 1, y));
    if (y > 0) result.set(toIndex(dim, x, y - 1));
    if (x < dim.width() - 1) result.set(toIndex(dim, x + 1, y));
    if (y < dim.height() - 1) result.set(toIndex(dim, x, y + 1));
    return result;
  }

  /**
   * @return a BitSet with all bordering (= orthogonal and diagonal) neighbors of index.
   * Checks that the fields are in bounds of dim.
   */
  public static BitSet borderingNeighbors(IDimension dim, int index, BitSet result) {
    int x = toX(dim, index);
    int y = toY(dim, index);
    setIfContains(dim, result, x - 1, y - 1);
    setIfContains(dim, result, x, y - 1);
    setIfContains(dim, result, x + 1, y - 1);
    setIfContains(dim, result, x + 1, y);
    setIfContains(dim, result, x + 1, y + 1);
    setIfContains(dim, result, x, y + 1);
    setIfContains(dim, result, x - 1, y + 1);
    setIfContains(dim, result, x - 1, y);
    return result;
  }

  public static BitSet neighbors(IDimension dim, int index) {
    return neighbors(dim, index, new BitSet());
  }

  /**
   * Expand orthogonally on all fields in set. Only adds fields where the predicate is true.
   *
   * @return the connected area as a BitSet
   */
  public static BitSet expand(IDimension dim, BitSet set, Predicate<Integer> predicate) {
    BitSet connectedFields = new BitSet();
    connectedFields.or(set);
    BitSet openFields = new BitSet();
    openFields.or(connectedFields);
    while (!openFields.isEmpty()) {
      int field = openFields.nextSetBit(0);
      openFields.set(field, false);
      BitSet neighbors = neighbors(dim, field);
      for (int neighbor = neighbors.nextSetBit(0); neighbor >= 0; neighbor = neighbors.nextSetBit(neighbor + 1)) {
        if (connectedFields.get(neighbor)) continue;
        if (predicate.apply(neighbor)) {
          connectedFields.set(neighbor);
          openFields.set(neighbor);
        }
      }
    }
    return connectedFields;
  }
}
