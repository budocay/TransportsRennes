/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.ybo.transportsrennes.activity.velos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import fr.ybo.transportsrennes.R;
import fr.ybo.transportsrennes.activity.commun.MenuAccueil;
import fr.ybo.transportsrennes.adapters.velos.VeloAdapter;
import fr.ybo.transportsrennes.application.TransportsRennesApplication;
import fr.ybo.transportsrennes.database.modele.VeloFavori;
import fr.ybo.transportsrennes.keolis.Keolis;
import fr.ybo.transportsrennes.keolis.modele.velos.Station;
import fr.ybo.transportsrennes.util.ErreurReseau;
import fr.ybo.transportsrennes.util.Formatteur;
import fr.ybo.transportsrennes.util.TacheAvecProgressDialog;

import java.util.*;

/**
 * Activité de type liste permettant de lister les stations de velos favorites.
 *
 * @author ybonnel
 */
public class ListStationsFavoris extends MenuAccueil.ListActivity {

    /**
     * Permet d'accéder aux apis keolis.
     */
    private final Keolis keolis = Keolis.getInstance();

    /**
     * Liste des stations.
     */
    private final List<Station> stations = Collections.synchronizedList(new ArrayList<Station>(10));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listvelofavoris);
        setListAdapter(new VeloAdapter(getApplicationContext(), stations));
        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressWarnings({"unchecked"})
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                VeloAdapter veloAdapter = (VeloAdapter) ((AdapterView<ListAdapter>) adapterView).getAdapter();
                Station station = veloAdapter.getItem(position);
                String lat = Double.toString(station.getLatitude());
                String lon = Double.toString(station.getLongitude());
                Uri uri = Uri.parse("geo:0,0?q=" + Formatteur.formatterChaine(station.name) + "+@" + lat + ',' + lon);
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        listView.setTextFilterEnabled(true);
        registerForContextMenu(listView);
        new TacheAvecProgressDialog<Void, Void, Void>(this, getString(R.string.dialogRequeteVeloStar)) {
            @Override
            protected void myDoBackground() throws ErreurReseau {
                List<VeloFavori> velosFavoris = TransportsRennesApplication.getDataBaseHelper()
                        .select(new VeloFavori());
                Collection<String> numbers = new ArrayList<String>(10);
                for (VeloFavori favori : velosFavoris) {
                    numbers.add(favori.number);
                }
                Collection<Station> stationsTmp = keolis.getStationByNumbers(numbers);
                synchronized (stations) {
                    stations.clear();
                    stations.addAll(stationsTmp);
                    Collections.sort(stations, new Comparator<Station>() {
                        public int compare(Station o1, Station o2) {
                            return o1.name.compareToIgnoreCase(o2.name);
                        }
                    });
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                super.onPostExecute(result);
            }
        }.execute((Void) null);
    }

    private static final int GROUP_ID = 0;
    private static final int MENU_REFRESH = Menu.FIRST;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(GROUP_ID, MENU_REFRESH, Menu.NONE, R.string.menu_refresh);
        item.setIcon(R.drawable.ic_menu_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_REFRESH) {
            new TacheAvecProgressDialog<Void, Void, Void>(this, getString(R.string.dialogRequeteVeloStar)) {
                @Override
                protected void myDoBackground() throws ErreurReseau {
                    List<VeloFavori> velosFavoris = TransportsRennesApplication.getDataBaseHelper().select(
                            new VeloFavori());
                    Collection<String> numbers = new ArrayList<String>(10);
                    for (VeloFavori favori : velosFavoris) {
                        numbers.add(favori.number);
                    }
                    Collection<Station> stationsTmp = keolis.getStationByNumbers(numbers);
                    synchronized (stations) {
                        stations.clear();
                        stations.addAll(stationsTmp);
                        Collections.sort(stations, new Comparator<Station>() {
                            public int compare(Station o1, Station o2) {
                                return o1.name.compareToIgnoreCase(o2.name);
                            }
                        });
                    }
                }

                @Override
                protected void onPostExecute(Void result) {
                    ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                    super.onPostExecute(result);
                }
            }.execute((Void) null);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Station station = (Station) getListAdapter().getItem(info.position);
            menu.setHeaderTitle(Formatteur.formatterChaine(station.name));
            menu.add(Menu.NONE, R.id.supprimerFavori, 0, getString(R.string.suprimerFavori));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Station station;
        VeloFavori veloFavori;
        switch (item.getItemId()) {
            case R.id.supprimerFavori:
                station = (Station) getListAdapter().getItem(info.position);
                veloFavori = new VeloFavori();
                veloFavori.number = station.number;
                TransportsRennesApplication.getDataBaseHelper().delete(veloFavori);
                ((VeloAdapter) getListAdapter()).getStations().remove(station);
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}