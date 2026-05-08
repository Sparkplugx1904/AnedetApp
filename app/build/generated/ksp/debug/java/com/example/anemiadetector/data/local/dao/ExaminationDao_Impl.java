package com.example.anemiadetector.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.anemiadetector.data.local.entity.ExaminationEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExaminationDao_Impl implements ExaminationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExaminationEntity> __insertionAdapterOfExaminationEntity;

  private final EntityDeletionOrUpdateAdapter<ExaminationEntity> __deletionAdapterOfExaminationEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ExaminationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExaminationEntity = new EntityInsertionAdapter<ExaminationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `examinations` (`id`,`timestamp`,`labelAnemia`,`labelNonAnemia`,`predictedLabel`,`confidence`,`imagePath`,`mode`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExaminationEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindDouble(3, entity.getLabelAnemia());
        statement.bindDouble(4, entity.getLabelNonAnemia());
        statement.bindString(5, entity.getPredictedLabel());
        statement.bindDouble(6, entity.getConfidence());
        statement.bindString(7, entity.getImagePath());
        statement.bindString(8, entity.getMode());
      }
    };
    this.__deletionAdapterOfExaminationEntity = new EntityDeletionOrUpdateAdapter<ExaminationEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `examinations` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExaminationEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM examinations";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ExaminationEntity examination,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfExaminationEntity.insertAndReturnId(examination);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ExaminationEntity examination,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfExaminationEntity.handle(examination);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExaminationEntity>> getAllExaminations() {
    final String _sql = "SELECT * FROM examinations ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"examinations"}, new Callable<List<ExaminationEntity>>() {
      @Override
      @NonNull
      public List<ExaminationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLabelAnemia = CursorUtil.getColumnIndexOrThrow(_cursor, "labelAnemia");
          final int _cursorIndexOfLabelNonAnemia = CursorUtil.getColumnIndexOrThrow(_cursor, "labelNonAnemia");
          final int _cursorIndexOfPredictedLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedLabel");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final List<ExaminationEntity> _result = new ArrayList<ExaminationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExaminationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final float _tmpLabelAnemia;
            _tmpLabelAnemia = _cursor.getFloat(_cursorIndexOfLabelAnemia);
            final float _tmpLabelNonAnemia;
            _tmpLabelNonAnemia = _cursor.getFloat(_cursorIndexOfLabelNonAnemia);
            final String _tmpPredictedLabel;
            _tmpPredictedLabel = _cursor.getString(_cursorIndexOfPredictedLabel);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            _item = new ExaminationEntity(_tmpId,_tmpTimestamp,_tmpLabelAnemia,_tmpLabelNonAnemia,_tmpPredictedLabel,_tmpConfidence,_tmpImagePath,_tmpMode);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExaminationEntity>> getByLabel(final String label) {
    final String _sql = "SELECT * FROM examinations WHERE predictedLabel = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, label);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"examinations"}, new Callable<List<ExaminationEntity>>() {
      @Override
      @NonNull
      public List<ExaminationEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLabelAnemia = CursorUtil.getColumnIndexOrThrow(_cursor, "labelAnemia");
          final int _cursorIndexOfLabelNonAnemia = CursorUtil.getColumnIndexOrThrow(_cursor, "labelNonAnemia");
          final int _cursorIndexOfPredictedLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedLabel");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final List<ExaminationEntity> _result = new ArrayList<ExaminationEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExaminationEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final float _tmpLabelAnemia;
            _tmpLabelAnemia = _cursor.getFloat(_cursorIndexOfLabelAnemia);
            final float _tmpLabelNonAnemia;
            _tmpLabelNonAnemia = _cursor.getFloat(_cursorIndexOfLabelNonAnemia);
            final String _tmpPredictedLabel;
            _tmpPredictedLabel = _cursor.getString(_cursorIndexOfPredictedLabel);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            _item = new ExaminationEntity(_tmpId,_tmpTimestamp,_tmpLabelAnemia,_tmpLabelNonAnemia,_tmpPredictedLabel,_tmpConfidence,_tmpImagePath,_tmpMode);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
