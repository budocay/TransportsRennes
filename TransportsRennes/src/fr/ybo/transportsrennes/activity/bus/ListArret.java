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
package fr.ybo.transportsrennes.activity.bus;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import fr.ybo.transportsrennes.R;
import fr.ybo.transportsrennes.activity.alerts.ListAlerts;
import fr.ybo.transportsrennes.activity.commun.MenuAccueil;
import fr.ybo.transportsrennes.adapters.bus.ArretAdapter;
import fr.ybo.transportsrennes.application.TransportsRennesApplication;
import fr.ybo.transportsrennes.database.modele.Ligne;
import fr.ybo.transportsrennes.util.IconeLigne;
import fr.ybo.transportsrennes.util.LogYbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Liste des arrêts d'une ligne de bus.
 *
 * @author ybonnel
 */
public class ListArret extends MenuAccueil.ListActivity {

    private static final LogYbo LOG_YBO = new LogYbo(ListArret.class);

    private Ligne myLigne;

    private Cursor currentCursor;

    private void closeCurrentCursor() {
        if (currentCursor != null && !currentCursor.isClosed()) {
            currentCursor.close();
        }
    }

    private String currentDirection;

    private void onDirectionClick() {
        StringBuilder requete = new StringBuilder();
        requete.append("SELECT Direction.id as directionId, Direction.direction as direction ");
        requete.append("FROM Direction, ArretRoute ");
        requete.append("WHERE Direction.id = ArretRoute.directionId");
        requete.append(" AND ArretRoute.ligneId = :ligneId ");
        requete.append("GROUP BY Direction.id, Direction.direction");
        Cursor cursor = TransportsRennesApplication.getDataBaseHelper().executeSelectQuery(requete.toString(), Collections.singletonList(myLigne.id));
        int directionIndex = cursor.getColumnIndex("direction");
        final List<String> items = new ArrayList<String>(5);
        while (cursor.moveToNext()) {
            items.add(cursor.getString(directionIndex));
        }
        cursor.close();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(fr.ybo.transportsrennes.R.string.chooseDirection));
        final String toutes = getString(fr.ybo.transportsrennes.R.string.Toutes);
        items.add(toutes);
        Collections.sort(items, new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (toutes.equals(o1)) {
                    return -1;
                }
                if (toutes.equals(o2)) {
                    return 1;
                }
                return o1.compareToIgnoreCase(o2);
            }
        });
        builder.setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                currentDirection = items.get(item).equals(toutes) ? null : items.get(item);
                construireListe();
                ((TextView) findViewById(fr.ybo.transportsrennes.R.id.directionArretCourante)).setText(items.get(item));
                findViewById(fr.ybo.transportsrennes.R.id.directionArretCouranteScroll).invalidate();
                getListView().invalidate();
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    private void construireCursor() {
        closeCurrentCursor();
        List<String> selectionArgs = new ArrayList<String>(2);
        selectionArgs.add(myLigne.id);
        StringBuilder requete = new StringBuilder();
        requete.append("select Arret.id as _id, Arret.nom as arretName,");
        requete.append(" Direction.direction as direction, ArretRoute.accessible as accessible, ArretRoute.macroDirection as macroDirection ");
        requete.append("from ArretRoute, Arret, Direction ");
        requete.append("where");
        requete.append(" ArretRoute.ligneId = :ligneId");
        requete.append(" and ArretRoute.arretId = Arret.id");
        requete.append(" and Direction.id = ArretRoute.directionId");
        if (currentDirection != null) {
            requete.append(" and Direction.direction = :direction");
            selectionArgs.add(currentDirection);
        }
        requete.append(" order by Direction.direction, ");
        if (orderDirection) {
            requete.append("ArretRoute.sequence");
        } else {
            requete.append("Arret.nom");
        }
        LOG_YBO.debug("Exécution de la requete permettant de récupérer les arrêts avec le temps avant le prochain");
        LOG_YBO.debug(requete.toString());
        currentCursor = TransportsRennesApplication.getDataBaseHelper().executeSelectQuery(requete.toString(), selectionArgs);
        LOG_YBO.debug("Exécution de la requete permettant de récupérer les arrêts terminée : " + currentCursor.getCount());
    }

    private void construireListe() {
        construireCursor();
        setListAdapter(new ArretAdapter(this, currentCursor, myLigne));
        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressWarnings({"unchecked"})
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Adapter arretAdapter = ((AdapterView<ListAdapter>) adapterView).getAdapter();
                Cursor cursor = (Cursor) arretAdapter.getItem(position);
                Intent intent = new Intent(ListArret.this, DetailArret.class);
                intent.putExtra("idArret", cursor.getString(cursor.getColumnIndex("_id")));
                intent.putExtra("nomArret", cursor.getString(cursor.getColumnIndex("arretName")));
                intent.putExtra("direction", cursor.getString(cursor.getColumnIndex("direction")));
                intent.putExtra("macroDirection", cursor.getInt(cursor.getColumnIndex("macroDirection")));
                intent.putExtra("ligne", myLigne);
                startActivity(intent);
            }
        });
        lv.setTextFilterEnabled(true);
        registerForContextMenu(lv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(fr.ybo.transportsrennes.R.layout.listearrets);
        myLigne = (Ligne) getIntent().getExtras().getSerializable("ligne");
        findViewById(fr.ybo.transportsrennes.R.id.directionArretCourante).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onDirectionClick();
            }
        });
        findViewById(R.id.googlemap).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(ListArret.this, ArretsOnMap.class);
                intent.putExtra("ligne", myLigne);
                if (currentDirection != null) {
                    intent.putExtra("direction", currentDirection);
                }
                startActivity(intent);
            }
        });
        ((TextView) findViewById(R.id.nomLong)).setText(myLigne.nomLong);
        ((ImageView) findViewById(R.id.iconeLigne)).setImageResource(IconeLigne.getIconeResource(myLigne.nomCourt));
        if (TransportsRennesApplication.hasAlert(myLigne.nomCourt)) {
            findViewById(R.id.alerte).setVisibility(View.VISIBLE);
            findViewById(R.id.alerte).setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(ListArret.this, ListAlerts.class);
                    intent.putExtra("ligne", myLigne);
                    startActivity(intent);
                }
            });
        } else {
            findViewById(R.id.alerte).setVisibility(View.GONE);
        }
        construireListe();
    }

    @Override
    protected void onDestroy() {
        closeCurrentCursor();
        super.onDestroy();
    }

    private static final int GROUP_ID = 0;
    private static final int MENU_ORDER = Menu.FIRST;

    private boolean orderDirection = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(GROUP_ID, MENU_ORDER, Menu.NONE, R.string.menu_orderByName);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(MENU_ORDER)
                .setTitle(orderDirection ? R.string.menu_orderByName : R.string.menu_orderBySequence);
        menu.findItem(MENU_ORDER).setIcon(orderDirection ? android.R.drawable.ic_menu_sort_alphabetically : android.R.drawable.ic_menu_sort_by_size);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_ORDER) {
            orderDirection = !orderDirection;
            construireListe();
            getListView().invalidate();
            return true;
        }
        return false;
    }
}