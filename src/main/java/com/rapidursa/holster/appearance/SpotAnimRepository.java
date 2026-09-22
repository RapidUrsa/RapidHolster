package com.rapidursa.holster.appearance;

import java.util.Collections;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The spotanim ("graphic") catalogue parsed from the live game cache. A
 * spotanim is how the game shows teleport swirls, spell
 * impacts and the home teleport's rune circle: a model, the animation that
 * plays it once, scaling, rotation, recolours and lighting tweaks.
 *
 * <p>{@code Actor.getGraphic()} exposes the active id; the parsed definition
 * supplies the corresponding model and animation data.
 */
@Slf4j
@Singleton
public class SpotAnimRepository
{
	/** One spotanim definition. Field names mirror the dumper's. */
	public static class Entry
	{
		public int m;
		public int a;
		public Integer rx;
		public Integer ry;
		public Integer rot;
		public Integer am;
		public Integer co;
		public short[] cf;
		public short[] cr;
		public short[] tf;
		public short[] tr;

		public int modelId()
		{
			return m;
		}

		public int animationId()
		{
			return a;
		}

		public int resizeX()
		{
			return rx == null ? 128 : rx;
		}

		public int resizeY()
		{
			return ry == null ? 128 : ry;
		}

		public int rotation()
		{
			return rot == null ? 0 : rot;
		}

		public int ambient()
		{
			return am == null ? 0 : am;
		}

		public int contrast()
		{
			return co == null ? 0 : co;
		}
	}

	private Map<String, Entry> spotanims = Collections.emptyMap();

	@Getter
	private String status = "not loaded";

	/**
	 * Builds the catalogue from the client's own loaded cache. No-op
	 * while the cache indexes are not available yet; callers retry.
	 */
	public void loadFromClient(net.runelite.api.Client client)
	{
		Map<String, Entry> live = LiveCacheParser.parseSpotAnims(client);
		if (live.isEmpty())
		{
			return;
		}
		spotanims = live;
		status = spotanims.size() + " spotanims (live cache)";
		log.info("Loaded {} spotanims from live cache", spotanims.size());
	}

	/** The definition for a graphic id, or null if unknown or not loaded. */
	public Entry get(int graphicId)
	{
		return spotanims.get(Integer.toString(graphicId));
	}

	public boolean isLoaded()
	{
		return !spotanims.isEmpty();
	}
}
