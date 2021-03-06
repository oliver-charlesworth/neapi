import React, { useEffect, useState } from "react";
import _ from "lodash";
import Page from "../components/Page";
import { Item, Format } from "../utils/model";
import { inventory } from "../utils/inventory";
import { headlineOffer } from "../utils/stuff";
import InventoryApp from "../components/InventoryApp";

const TASTER_MENU_SIZE = 20;

const ThisPage = (): JSX.Element => {
  const [items, setItems] = useState<Array<Item>>([]);

  // TODO - is there a better way to avoid this being captured by SSG?
  useEffect(() => {
    const sample = generateFairTasterMenu(inventory.items);
    setItems(_.sortBy(_.sortBy(sample, item => item.name), item => item.brewery.shortName));
  }, []);

  return (
    <Page
      title="Taster menu"
      desc="Taster menu of beer prices from across the UK"
      longDesc={
        (
          <>
            <p>
              We&apos;ve put together a randomly curated selection of {TASTER_MENU_SIZE} beers to inspire you.
            </p>
            <p>
              Refresh the page to get another selection!
            </p>
            <p>
              Every item here can be delivered directly to your doorstep from the brewery&apos;s online shop.
            </p>
          </>
        )
      }
      breweries={inventory.breweries}
    >
      <InventoryApp inventory={{ ...inventory, items }} />
    </Page>
  );
};

// Avoid over-representing breweries that have a ton of beers.
const generateFairTasterMenu = (items: Array<Item>): Array<Item> => {
  // Remove inappropriate items for a taster menu
  const relevant = _.filter(items, item => headlineOffer(item).format !== Format.Keg && !item.mixed && item.available);

  const byBrewery = _.groupBy(relevant, item => item.brewery.id);

  // Pick breweries with *almost* uniform distribution.
  // We allow breweries with lots of beers to feature *slightly* more.
  const breweryPicks = _.shuffle(
    _.flatten(
      _.map(byBrewery, (items, brewery) => {
        const count = _.size(items);
        const rep =
          (count >= 10) ? 6 :
          (count >= 5) ? 5 :
          4;
        return _.times(rep, () => brewery);
      })
    )
  );

  // TODO - what if TASTER_MENU_SIZE < num items?
  const picked = new Set<Item>();
  let idx = 0;
  while (picked.size < TASTER_MENU_SIZE) {
    const item = _.sample(byBrewery[breweryPicks[idx++]]);
    if (item !== undefined) {
      picked.add(item);
    }
  }
  return Array.from(picked);
};

export default ThisPage;
