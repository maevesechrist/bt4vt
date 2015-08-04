/*
 * Copyright 2015 Ben Sechrist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bt4vt.fragment;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bt4vt.R;
import com.bt4vt.adapter.DepartureArrayAdapter;
import com.bt4vt.async.AsyncCallback;
import com.bt4vt.async.DepartureAsyncTask;
import com.bt4vt.repository.FirebaseService;
import com.bt4vt.repository.TransitRepository;
import com.bt4vt.repository.domain.Departure;
import com.bt4vt.repository.domain.Route;
import com.bt4vt.repository.domain.Stop;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.Scopes;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.List;

import roboguice.fragment.RoboDialogFragment;
import roboguice.inject.InjectView;

/**
 * Shows the scheduled departures for the given stop in a dialog.
 *
 * @author Ben Sechrist
 */
public class ScheduledDeparturesDialogFragment extends RoboDialogFragment
    implements AsyncCallback<List<Departure>>, View.OnClickListener {

  private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
  private static final int REQUEST_AUTHORIZATION = 2000;

  private static final String STOP_FORMAT = "Stop: %s";
  private static final String ROUTE_FORMAT = "Route: %s";
  private static final String ALL_ROUTES = "ALL";

  @Inject
  private LayoutInflater inflater;

  @Inject
  private TransitRepository transitRepository;

  @Inject
  private FirebaseService firebaseService;

  @InjectView(R.id.stop_text)
  private TextView stopTextView;

  @InjectView(R.id.route_text)
  private TextView routeTextView;

  @InjectView(R.id.list_view)
  private ListView listView;

  @InjectView(R.id.departure_loading_view)
  private View loadingView;

  @InjectView(R.id.empty_departures_view)
  private View emptyDeparturesView;

  @InjectView(R.id.button_favorite_stop)
  private ImageButton favoriteButton;

  private Stop stop;
  private Route route;

  public static ScheduledDeparturesDialogFragment newInstance(Stop stop, Route route) {
    ScheduledDeparturesDialogFragment fragment = new ScheduledDeparturesDialogFragment();
    fragment.stop = stop;
    fragment.route = route;
    return fragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    return inflater.inflate(R.layout.departures_dialog, container);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setFavButtonText();

    favoriteButton.setOnClickListener(this);

    loadingView.setVisibility(View.VISIBLE);

    stopTextView.setText(String.format(STOP_FORMAT, stop.toString()));
    String routeText;
    if (route != null) {
      routeText = route.toString();
    } else {
      routeText = ALL_ROUTES;
    }
    routeTextView.setText(String.format(ROUTE_FORMAT, routeText));

    new DepartureAsyncTask(transitRepository, stop, route, this).execute();

    emptyDeparturesView.findViewById(R.id.refresh_departures_button).setOnClickListener(this);
  }

  @Override
  public void onSuccess(List<Departure> departures) {
    listView.setAdapter(new DepartureArrayAdapter(getActivity(), departures));
    listView.setEmptyView(emptyDeparturesView);
    loadingView.setVisibility(View.INVISIBLE);
  }

  @Override
  public void onException(Exception e) {
    e.printStackTrace();
    listView.setEmptyView(emptyDeparturesView);
    loadingView.setVisibility(View.INVISIBLE);
    Toast.makeText(getActivity(), "Error fetching departures", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.refresh_departures_button:
        listView.setEmptyView(null);
        loadingView.setVisibility(View.VISIBLE);
        new DepartureAsyncTask(transitRepository, stop, route, this).execute();
        break;
      case R.id.button_favorite_stop:
        onFavClick();
        break;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_PICK_ACCOUNT || requestCode == REQUEST_AUTHORIZATION) {
      if (resultCode == Activity.RESULT_OK) {
        new FetchGoogleTokenTask(getActivity(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
            .execute();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        // The account picker dialog closed without selecting an account.
        Toast.makeText(getActivity(), "You must login to favorite stops", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void onFavClick() {
    if (!firebaseService.isAuthenticated()) {
      String[] accountTypes = new String[]{"com.google"};
      Intent intent = AccountPicker.newChooseAccountIntent(null, null,
          accountTypes, false, null, null, null, null);
      startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    } else {
      if (transitRepository.isFavorited(stop)) {
        transitRepository.unfavoriteStop(stop);
      } else {
        transitRepository.favoriteStop(stop);
      }
    }
    setFavButtonText();
  }

  private void setFavButtonText() {
    if (transitRepository.isFavorited(stop)) {
      favoriteButton.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.heart));
    } else {
      favoriteButton.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.empty_heart));
    }
  }

  private class FetchGoogleTokenTask extends AsyncTask<Void, Void, String> {

    private final Context context;
    private final String email;

    private FetchGoogleTokenTask(Context context, String email) {
      this.context = context;
      this.email = email;
    }

    @Override
    protected String doInBackground(Void... voids) {
      try {
        String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
        return GoogleAuthUtil.getToken(context, email, scope);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (UserRecoverableAuthException e) {
        startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
      } catch (GoogleAuthException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(String token) {
      if (token != null) {
        firebaseService.loginGoogle(token);
        onFavClick();
      }
    }
  }
}
